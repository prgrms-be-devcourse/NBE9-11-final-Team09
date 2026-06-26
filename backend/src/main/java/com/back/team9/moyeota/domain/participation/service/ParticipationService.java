package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.dto.MyParticipationResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.payment.entity.Payment;
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
import org.springframework.transaction.annotation.Propagation;


import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final PaymentRepository paymentRepository;
    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final SeatRedisService seatRedisService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ============================== 1. 참여 신청 ==============================
    @Transactional
    public ParticipationResponse createParticipation(Long memberId, ParticipationCreateRequest request) {

        // 펀딩 조회
        Funding funding = fundingRepository.findById(request.fundingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 펀딩 상태 확인
        validateFundingStatus(funding);

        // 중복 참여 검증
        if (participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                funding.getFundingId(),
                memberId,
                List.of(
                        ParticipationPaymentStatus.PENDING,
                        ParticipationPaymentStatus.ACTIVE,
                        ParticipationPaymentStatus.COMPLETED
                )
        )) {
            throw new BusinessException(ErrorCode.DUPLICATE_PARTICIPATION);
        }

        // 정원 확인
        long currentParticipants = participationRepository
                .countByFunding_FundingIdAndPaymentStatusIn(
                        funding.getFundingId(),
                        List.of(
                                ParticipationPaymentStatus.PENDING,
                                ParticipationPaymentStatus.ACTIVE
                        )
                );
        if (currentParticipants >= funding.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.FUNDING_RECRUITMENT_CLOSED);
        }
        Seat outboundSeat = getValidatedSeat(
                request.outboundSeatId(),
                funding.getFundingId(),
                Direction.OUTBOUND
        );
        validateReturnSeatRequirement(
                funding.getTripType(),
                request.returnSeatId()
        );
        Seat returnSeat = null;
        if (request.returnSeatId() != null) {
            returnSeat = getValidatedSeat(
                    request.returnSeatId(),
                    funding.getFundingId(),
                    Direction.RETURN
            );
        }
        // 기존 참여 이력 확인
        Optional<Participation> existingParticipation = participationRepository
                .findByFunding_FundingIdAndMember_MemberId(funding.getFundingId(), memberId);

        if (existingParticipation.isPresent()) {
            Participation participation = existingParticipation.get();

            if (participation.getStatus() == ParticipationStatus.CANCELED) {
                participation.reapply(outboundSeat, returnSeat);
                return ParticipationResponse.from(participation);
            }
        }

        // 신규 참여 생성
        Participation participation = Participation.create(
                funding,
                member,
                outboundSeat,
                returnSeat
        );

        participationRepository.save(participation);

        return ParticipationResponse.from(participation);
    }

    // 펀딩 상태 확인
    private void validateFundingStatus(Funding funding) {
        FundingStatus status = funding.getStatus();

        if (status == FundingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.FUNDING_CANCELLED);
        }
        if (status == FundingStatus.COMPLETED || status == FundingStatus.FAILED) {
            throw new BusinessException(ErrorCode.FUNDING_RECRUITMENT_CLOSED);
        }
    }

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

    //좌석 조회 + 검증
    private Seat getValidatedSeat(Long seatId, Long fundingId, Direction expectedDirection) {

        Seat seat = seatRepository.findByIdWithPathinfoAndFunding(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        if (!seat.getPathinfo().getFunding().getFundingId().equals(fundingId)) {
            throw new BusinessException(ErrorCode.SEAT_NOT_IN_PATH);
        }

        if (seat.getPathinfo().getDirection() != expectedDirection) {
            throw new BusinessException(ErrorCode.SEAT_NOT_IN_PATH);
        }

        return seat;
    }

    // 오는편 좌석 필요 여부 확인
    private void validateReturnSeatRequirement(TripType tripType, Long returnSeatId) {

        if (tripType == TripType.ROUND && returnSeatId == null) {
            throw new BusinessException(ErrorCode.ROUND_TRIP_SEAT_REQUIRED);
        }
        if (tripType == TripType.ONE_WAY && returnSeatId != null) {
            throw new BusinessException(ErrorCode.ONE_WAY_RETURN_SEAT_NOT_ALLOWED);
        }
    }

    // ============================== 2. 참여 취소 ==============================
    @Transactional
    public void cancelParticipation(Long memberId, Long participationId) {

        Participation participation = participationRepository
                .findByParticipationIdAndMember_MemberId(participationId, memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        if (participation.getStatus() == ParticipationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELED_PARTICIPATION);
        }

        // PENDING 상태 (결제 전 취소) → 날짜 제한 없이 즉시 취소 + Redis HOLD 해제만
        if (participation.getPaymentStatus() == ParticipationPaymentStatus.PENDING) {
            Long outboundSeatId = participation.getOutboundSeat().getSeatId();
            releaseSeatHoldSafely(outboundSeatId, memberId);

            Seat returnSeat = participation.getReturnSeat();
            if (returnSeat != null) {
                releaseSeatHoldSafely(returnSeat.getSeatId(), memberId);
            }

            participation.cancel();
            return;
        }

        // ACTIVE 상태 (보증금 결제 완료 후 취소) → 7일/10일 정책 적용
        LocalDateTime departureTime = participation.getOutboundSeat()
                .getPathinfo()
                .getDepartureTime();

        LocalDateTime now = LocalDateTime.now(clock);

        LocalDateTime cancelDeadline = departureTime
                .toLocalDate()
                .minusDays(7)
                .atStartOfDay();

        if (now.isAfter(cancelDeadline)) {
            throw new BusinessException(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED);
        }

        LocalDateTime refundDeadline = departureTime
                .toLocalDate()
                .minusDays(10)
                .atStartOfDay();

        if (now.isBefore(refundDeadline)) {
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
    @Transactional(readOnly = true)
    public List<MyParticipationResponse> getMyParticipations(Long memberId) {

        return participationRepository
                .findByMember_MemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(MyParticipationResponse::from)
                .toList();
    }

    // 참여자 목록 조회 (방장용)
    @Transactional(readOnly = true)
    public List<ParticipationListResponse> getParticipations(Long memberId, Long fundingId) {

        // 펀딩 조회
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.FUNDING_NOT_FOUND)
                );

        // 방장 여부 확인
        if (!funding.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FUNDING_FORBIDDEN);
        }

        // 참여자 목록 조회
        List<Participation> participations =
                participationRepository.findByFunding_FundingId(fundingId);

        return participations.stream()
                .map(ParticipationListResponse::from)
                .toList();
    }

    // ============================== 4. 결제 완료 후 좌석 확정 ==============================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmAfterPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Participation participation = payment.getParticipation();

        // 이미 결제가 완료된 경우 중복 처리 방지 (멱등성 보장)
        if (participation.getPaymentStatus() == ParticipationPaymentStatus.ACTIVE ||
                participation.getPaymentStatus() == ParticipationPaymentStatus.COMPLETED) {
            return;
        }

        Long memberId = participation.getMember().getMemberId();

        // 가는편 좌석 Redis HOLD 유효성 확인
        Long outboundSeatId = participation.getOutboundSeat().getSeatId();
        if (!seatRedisService.isHeldBy(outboundSeatId, memberId)) {
            throw new BusinessException(ErrorCode.SEAT_HOLD_EXPIRED);
        }

        // 왕복인 경우 오는편 좌석도 확인
        Seat returnSeat = participation.getReturnSeat();
        if (returnSeat != null && !seatRedisService.isHeldBy(returnSeat.getSeatId(), memberId)) {
            throw new BusinessException(ErrorCode.SEAT_HOLD_EXPIRED);
        }

        // HOLD 유효 → 좌석 BOOKED 확정 + Redis HOLD 해제
        participation.getOutboundSeat().book(participation);
        releaseSeatHoldSafely(outboundSeatId, memberId);

        if (returnSeat != null) {
            returnSeat.book(participation);
            releaseSeatHoldSafely(returnSeat.getSeatId(), memberId);
        }
        participation.confirmPayment();
    }

    // ============================== 5. 결제 실패 시 참여 취소 ==============================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelByPaymentFailure(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Participation participation = payment.getParticipation();
        Long memberId = participation.getMember().getMemberId();

        // 결제 대기(PENDING) 상태가 아닌 경우 취소 처리 방지
        if (participation.getPaymentStatus() != ParticipationPaymentStatus.PENDING) {
            return;
        }

        participation.cancel();

        // Redis HOLD 해제 시도 (이미 만료됐을 수 있음)
        Long outboundSeatId = participation.getOutboundSeat().getSeatId();
        releaseSeatHoldSafely(outboundSeatId, memberId);

        Seat returnSeat = participation.getReturnSeat();
        if (returnSeat != null) {
            releaseSeatHoldSafely(returnSeat.getSeatId(), memberId);
        }
    }

    // ============================== 6. 잔액 결제 완료 시 COMPLETED 전환 ==============================
    @Transactional
    public void completeBalancePayment(Long participationId) {

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        // 이미 COMPLETED인 경우 중복 처리 방지 (멱등성 보장)
        if (participation.getPaymentStatus() == ParticipationPaymentStatus.COMPLETED) {
            return;
        }

        if (participation.getPaymentStatus() != ParticipationPaymentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_PARTICIPATION_STATUS);
        }
        participation.completePayment();
    }
}
