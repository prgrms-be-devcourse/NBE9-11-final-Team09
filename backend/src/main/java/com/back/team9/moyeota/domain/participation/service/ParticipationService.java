package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.domain.seat.service.SeatRedisService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final SeatRedisService seatRedisService;

    // ======================== 1. 참여 신청========================
    @Transactional
    public ParticipationResponse createParticipation(Long memberId, ParticipationCreateRequest request) {

        //펀딩 조회 - 존재하지 않으면 FND001
        Funding funding = fundingRepository.findById(request.fundingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        //회원 조회 - 존재하지 않으면 USR001
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 펀딩 상태 확인
        validateFundingStatus(funding);

        //중복 참여 검증 - 이미 참여 중이면 PTC002
        if (participationRepository.existsByFunding_FundingIdAndMember_MemberId(
                funding.getFundingId(), memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PARTICIPATION);
        }

        //정원 확인 - 현재 ACTIVE 참여자 수가 정원 이상이면 PTC003
        long currentParticipants = participationRepository
                .countByFunding_FundingIdAndStatus(funding.getFundingId(), ParticipationStatus.ACTIVE);
        if (currentParticipants >= funding.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.FUNDING_RECRUITMENT_CLOSED);
        }

        //가는편 좌석 확인 (필수, 방향: OUTBOUND)
        Seat outboundSeat = getValidatedSeat(
                request.outboundSeatId(),
                funding.getFundingId(),
                Direction.OUTBOUND
        );

        //오는편 좌석 필요 여부 확인
        validateReturnSeatRequirement(
                funding.getTripType(),
                request.returnSeatId()
        );

        //오는편 좌석 확인 (왕복인 경우만, 방향: RETURN)
        Seat returnSeat = null;

        if (request.returnSeatId() != null) {
            returnSeat = getValidatedSeat(
                    request.returnSeatId(),
                    funding.getFundingId(),
                    Direction.RETURN
            );
        }

        //참여 생성 및 저장
        // 이 시점에 participationId가 생성됨 (Seat.book()에서 필요)
        Participation participation = Participation.create(
                funding,
                member,
                outboundSeat,
                returnSeat
        );

        participationRepository.save(participation);

        //좌석 예약 확정
        // Seat 상태를 BOOKED로 변경 (최종 동시성 방어선)
        //  이미 BOOKED 상태면 Seat.book() 내부에서 SEAT_ALREADY_OCCUPIED(SEA002) 예외 발생
        outboundSeat.book(participation);
        if (returnSeat != null) {
            returnSeat.book(participation);
        }

        // 10. Redis HOLD 해제 - 더 이상 임시 선점 상태가 아니므로 Redis 키 삭제
        seatRedisService.releaseSeat(
                outboundSeat.getSeatId(),
                memberId
        );

        if (returnSeat != null) {
            seatRedisService.releaseSeat(
                    returnSeat.getSeatId(),
                    memberId
            );
        }

        return ParticipationResponse.from(participation);
    }

    // 펀딩 상태 확인
    private void validateFundingStatus(Funding funding) {
        FundingStatus status = funding.getStatus();

        // 취소된 펀딩인지 확인
        if (status == FundingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.FUNDING_CANCELLED);
        }
        // 모집이 종료된 펀딩인지 확인
        if (status == FundingStatus.COMPLETED || status == FundingStatus.FAILED) {
            throw new BusinessException(ErrorCode.FUNDING_RECRUITMENT_CLOSED);
        }
    }

    //좌석 조회 + 검증 (outbound/return 공통 로직)
    private Seat getValidatedSeat(Long seatId, Long fundingId, Direction expectedDirection) {

        //좌석 조회
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        //예약 가능한 좌석인지 확인 (이미 BOOKED면 SEA002)
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        //해당 펀딩의 좌석인지 확인
        if (!seat.getPathinfo().getFunding().getFundingId().equals(fundingId)) {
            throw new BusinessException(ErrorCode.SEAT_NOT_IN_PATH);
        }

        //좌석의 노선 방향이 기대 방향과 일치하는지 확인(가는편/오는편 방향 확인)
        if (seat.getPathinfo().getDirection() != expectedDirection) {
            throw new BusinessException(ErrorCode.SEAT_NOT_IN_PATH);
        }

        return seat;
    }

    // 오는편 좌석 필요 여부 확인
    private void validateReturnSeatRequirement(TripType tripType, Long returnSeatId) {
        // 왕복은 오는편 좌석 필수
        if (tripType == TripType.ROUND && returnSeatId == null) {
            throw new BusinessException(ErrorCode.ROUND_TRIP_SEAT_REQUIRED);
        }
        // 편도는 오는편 좌석 선택 불가
        if (tripType == TripType.ONE_WAY && returnSeatId != null) {
            throw new BusinessException(ErrorCode.ONE_WAY_RETURN_SEAT_NOT_ALLOWED);
        }
    }
}