package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;


import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final SeatRedisService seatRedisService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ============================== 1. 참여 신청 ==============================
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
        // 이미 BOOKED 상태면 Seat.book() 내부에서 SEAT_ALREADY_OCCUPIED(SEA002) 예외 발생
        outboundSeat.book(participation);
        if (returnSeat != null) {
            returnSeat.book(participation);
        }

        // Redis HOLD 해제 - 더 이상 임시 선점 상태가 아니므로 Redis 키 삭제
        // 각 좌석을 독립적으로 처리 - 한쪽 실패가 다른 쪽 처리를 막지 않도록 함
        // HOLD 키는 5분 TTL이 설정되어 있어-> 삭제 실패해도 자동 만료됨
        releaseSeatHoldSafely(outboundSeat.getSeatId(), memberId);

        if (returnSeat != null) {
            releaseSeatHoldSafely(returnSeat.getSeatId(), memberId);
        }

        return ParticipationResponse.from(participation);
    }  // ← createParticipation() 메서드 끝

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

     // 좌석별로 독립 처리하여, 한 좌석의 실패가 다른 좌석 처리를 막지 않음
    private void releaseSeatHoldSafely(Long seatId, Long memberId) {
        boolean released = seatRedisService.releaseSeat(seatId, memberId);
        if (!released) {
            log.warn(
                    "좌석 HOLD 해제 실패 (이미 만료/타인 소유 또는 Redis 장애 - TTL로 자동 정리됨) - seatId: {}, memberId: {}",
                    seatId,
                    memberId
            );
        }
    }

    //좌석 조회 + 검증 (outbound/return 공통 로직)
    private Seat getValidatedSeat(Long seatId, Long fundingId, Direction expectedDirection) {

        //좌석 조회
        Seat seat = seatRepository.findByIdWithPathinfoAndFunding(seatId)
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


    // ============================== 2. 참여 취소 ==============================
    @Transactional
    public void cancelParticipation(Long memberId, Long participationId) {

        // 본인 참여 내역 조회 - 없거나 본인 것이 아니면 PTC001
        Participation participation = participationRepository
                .findByParticipationIdAndMember_MemberId(participationId, memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        // 이미 취소된 참여인지 확인 - 중복 취소 방지
        if (participation.getStatus() == ParticipationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELED_PARTICIPATION);
        }

        // 가는편 좌석의 출발 시각을 기준으로 취소 가능 시점 판단
        LocalDateTime departureTime = participation.getOutboundSeat()
                .getPathinfo()
                .getDepartureTime();

        // 출발 7일 전 자정 = 취소 가능 마감 시점
        LocalDateTime cancelDeadline = departureTime
                .toLocalDate()
                .minusDays(7)
                .atStartOfDay();

        // 출발 7일 전 자정 이후엔 참여 취소 요청 자체를 허용하지 않음
        // 프론트에서는 취소 버튼을 숨기고, 백엔드에서도 예외로 한 번 더 방어(PTC006)
        if (LocalDateTime.now(clock).isAfter(cancelDeadline)) {
            throw new BusinessException(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED);
        }

        // 출발 10일 전 자정 = 보증금 환불 가능 마감 시점
        LocalDateTime refundDeadline = departureTime
                .toLocalDate()
                .minusDays(10)
                .atStartOfDay();

        // 환불 대상(10일 전 이전 취소)일 때만 이벤트 발행
        if (LocalDateTime.now(clock).isBefore(refundDeadline)) {
            eventPublisher.publishEvent(
                    new ParticipationCancelledEvent(participationId)
            );
        }

        participation.cancel();
        participation.getOutboundSeat().release();

        if (participation.getReturnSeat() != null) {
            participation.getReturnSeat().release();
        }
    }


    // ============================== 3. 참여자 목록 조회 ==============================
    // 참여자 목록 조회 (방장용)
    @Transactional(readOnly = true)
    public List<ParticipationListResponse> getParticipations(
            Long memberId, //요청자(방장) ID
            Long fundingId //조회할 펀딩 ID
    ) {

        // 펀딩 조회 - 존재하지 않으면 FND001
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.FUNDING_NOT_FOUND)
                );

        // 방장 여부 확인 - 방장이 아니면 FND007
        if (!funding.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FUNDING_FORBIDDEN);
        }

        // 참여자 목록 조회
        List<Participation> participations =
                participationRepository.findByFunding_FundingId(fundingId);

        // DTO 변환 후 반환
        return participations.stream()
                .map(ParticipationListResponse::from)
                .toList();
    }
}