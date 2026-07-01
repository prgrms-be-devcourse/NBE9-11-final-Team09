package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.policy.FundingPricePolicy;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.domain.payment.client.TossConfirmResponse;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentPrepareResponse;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentWriter paymentWriter;
    private final ParticipationRepository participationRepository;
    private final ParticipationService participationService;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final PaymentRedisService paymentRedisService;

    @Value("${admin.email}")
    private String adminEmail;

    @Transactional

    public PaymentResponse confirmDeposit(PaymentConfirmRequest request, Long memberId) {
        return confirmPayment(request, PaymentType.DEPOSIT, memberId);
    }

    @Transactional
    public PaymentResponse confirmBalance(PaymentConfirmRequest request, Long memberId) {
        PaymentResponse response = confirmPayment(request, PaymentType.BALANCE, memberId);
        participationService.completeBalancePayment(request.participationId());
        return response;
    }

    private PaymentResponse confirmPayment(PaymentConfirmRequest request, PaymentType paymentType, Long memberId) {

        log.info("결제 승인 요청 (participationId={}, paymentType={}, amount={})",
                request.participationId(), paymentType, request.amount());

        Payment pendingPayment = paymentRepository
                .findByParticipation_ParticipationIdAndStatus(request.participationId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!pendingPayment.getParticipation().getMember().getMemberId().equals(memberId)) {
            log.warn("결제 접근 권한 없음 (paymentId={}, memberId={})",
                    pendingPayment.getPaymentId(), memberId);
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }
        if (paymentRepository.findByTossPaymentKey(request.paymentKey()).isPresent()) {
            log.warn("중복 결제 시도 (paymentKey={})",
                    request.paymentKey());
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        Participation participation = pendingPayment.getParticipation();
        if (request.amount().compareTo(pendingPayment.getAmount()) != 0) {
            log.warn("결제 금액 불일치 (paymentId={}, requestAmount={}, expectedAmount={})",
                    pendingPayment.getPaymentId(),
                    request.amount(),
                    pendingPayment.getAmount());
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        TossConfirmResponse tossResponse = tossPaymentClient.confirm(
                request.paymentKey(),
                pendingPayment.getOrderId(),
                request.amount()
        );

        pendingPayment.confirm(paymentType, tossResponse.paymentKey());
        paymentWriter.save(pendingPayment);

        try {
            participationService.confirmAfterPayment(pendingPayment.getPaymentId());
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.SEAT_HOLD_EXPIRED) {
                pendingPayment.startRefund();
                paymentWriter.saveRefundStatus(pendingPayment);
                try {
                    tossPaymentClient.cancel(pendingPayment.getTossPaymentKey(), "좌석 선점 시간 만료로 인한 자동 취소");
                    pendingPayment.completeRefund();
                    paymentWriter.saveRefundStatus(pendingPayment);
                    participationService.cancelByPaymentFailure(pendingPayment.getPaymentId());
                } catch (Exception cancelEx) {
                    log.error("좌석 만료 환불 실패 (paymentId={})",
                            pendingPayment.getPaymentId(), cancelEx);
                    try {
                        mailService.send(
                                adminEmail,
                                "[긴급] SEAT_HOLD_EXPIRED 환불 실패 — 수동 처리 필요",
                                "paymentId: " + pendingPayment.getPaymentId() + " 좌석 만료 자동 취소 중 Toss cancel 실패. DB는 REFUND_PENDING 상태."
                        );
                    } catch (Exception mailEx) {
                        log.error("어드민 알림 발송 실패: {}", mailEx.getMessage(), mailEx);
                    }
                }
            }
            throw e;
        }
        try {
            notificationService.sendMimeMessage(
                    participation.getMember().getMemberId(),
                    participation.getFunding().getFundingId(),
                    NotificationType.PAYMENT_COMPLETED
            );
        } catch (Exception e) {
            log.warn("결제 완료 알림 발송 실패 (paymentId={}): {}", pendingPayment.getPaymentId(), e.getMessage(), e);
        }

        log.info("결제 승인 완료 (paymentId={}, paymentType={}, amount={})",
                pendingPayment.getPaymentId(), paymentType, pendingPayment.getAmount());

        return PaymentResponse.from(pendingPayment);
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, PaymentRefundRequest request, Long memberId) {

        log.info("환불 요청 (paymentId={}, memberId={})",
                paymentId,
                memberId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!payment.getParticipation().getMember().getMemberId().equals(memberId)) {
            log.warn("환불 접근 권한 없음 (paymentId={}, memberId={})",
                    paymentId, memberId);
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.REFUND_PENDING) {

            log.warn("이미 환불된 결제 (paymentId={}, status={})",
                    paymentId, payment.getStatus());

            throw new BusinessException(ErrorCode.ALREADY_REFUNDED);
        }
        if (payment.getStatus() != PaymentStatus.PAID) {

            log.warn("환불 불가 상태 (paymentId={}, status={})",
                    paymentId, payment.getStatus());

            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        payment.startRefund();
        paymentWriter.save(payment);
        try {
            tossPaymentClient.cancel(
                    payment.getTossPaymentKey(),
                    request.cancelReason()
            );
        } catch (Exception e) {
            log.error("환불 실패 (paymentId={}, reason={})",
                    paymentId, request.cancelReason(), e);
            throw e;
        }

        payment.completeRefund();
        Payment updatedPayment = paymentWriter.save(payment);

        log.info("환불 완료 (paymentId={}, amount={})",
                updatedPayment.getPaymentId(),
                updatedPayment.getAmount());

        return PaymentResponse.from(updatedPayment);
    }

    @Transactional
    public void refundByParticipationId(Long participationId) {

        log.info("참여 취소 환불 시작 (participationId={})",
                participationId);

        List<Payment> payments = paymentRepository.findByParticipation_ParticipationId(participationId);
        if (payments.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // TODO: 사용자 자진 취소와 관리자/방장에 의한 강제 취소가 동일 로직을 공유함.
        // 사용자 취소 정책: 출발 -7일 이전까지 취소 가능, -10일 이전에는 보증금 환불 O, -10일~-7일은 환불 없이 취소만 가능.
        // -10일 이전(환불 가능 시점)에는 펀딩이 CONFIRMED 전이라 BALANCE 납부 불가 → 현재는 문제없음.
        // 추후 두 경로를 분리하여 각각의 환불 정책을 독립적으로 적용할 필요 있음.
        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.PAID) {
                continue;
            }
            payment.startRefund();
            paymentWriter.save(payment);

            try {
                tossPaymentClient.cancel(payment.getTossPaymentKey(), "참여 취소로 인한 환불");
            } catch (Exception e) {
                log.error("참여 취소 환불 실패 (paymentId={}, amount={}, reason={})",
                        payment.getPaymentId(),
                        payment.getAmount(),
                        e.getMessage(),
                        e);
                throw e;
            }

            payment.completeRefund();
            paymentWriter.save(payment);

            log.info("참여 취소 환불 완료 (paymentId={}, amount={})",
                    payment.getPaymentId(), payment.getAmount());
        }

    }

    @Transactional
    public PaymentPrepareResponse prepare(Long participationId, Long memberId) {

        log.info("결제 준비 요청 (participationId={}, memberId={})",
                participationId,
                memberId);

        paymentRedisService.lockPrepare(participationId);
        try {
            Participation participation = participationRepository.findById(participationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

            if (!participation.getMember().getMemberId().equals(memberId)) {
                log.warn("결제 준비 접근 권한 없음 (participationId={}, memberId={})",
                        participationId, memberId);
                throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
            }

            Funding funding = participation.getFunding();
            BigDecimal deposit = FundingPricePolicy.calculateRoundedPrice(
                            funding.getTotalPrice(),
                            funding.getMaxParticipants() + 1
                    )
                    .divide(BigDecimal.valueOf(2), 0, RoundingMode.CEILING);

            BigDecimal amount;
            if (participation.getFinalAmount().compareTo(BigDecimal.ZERO) > 0) {
                amount = participation.getFinalAmount().subtract(deposit);
            } else {
                amount = deposit;
            }

            List<Payment> existingPendings = paymentRepository.findAllByParticipation_ParticipationIdAndStatus(participationId,
                    PaymentStatus.PENDING);
            paymentRepository.deleteAll(existingPendings);

            String orderId = UUID.randomUUID().toString();
            Payment payment = Payment.builder()
                    .participation(participation)
                    .orderId(orderId)
                    .amount(amount)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentWriter.save(payment);

            log.info("결제 준비 완료 (participationId={}, orderId={}, amount={})",
                    participationId,
                    orderId,
                    amount);

            return new PaymentPrepareResponse(orderId, amount);
        } finally {
            paymentRedisService.unlockPrepare(participationId);
        }
    }

    @Transactional
    public void expirePayment(Payment payment) {

        log.info("결제 만료 처리 (paymentId={}, amount={})",
                payment.getPaymentId(), payment.getAmount());

        payment.expire();
        paymentWriter.save(payment);
        participationService.cancelByPaymentFailure(payment.getPaymentId());
    }
}
