package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.Participation;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
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
    private final Clock clock;
    private final ParticipationService participationService;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse confirmDeposit(PaymentConfirmRequest request) {
        return confirmPayment(request, PaymentType.DEPOSIT);
    }

    @Transactional
    public PaymentResponse confirmBalance(PaymentConfirmRequest request) {
        return confirmPayment(request, PaymentType.BALANCE);
    }

    private PaymentResponse confirmPayment(PaymentConfirmRequest request, PaymentType paymentType) {

        Payment pendingPayment = paymentRepository
                .findByParticipation_ParticipationIdAndStatus(request.participationId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (paymentRepository.findByTossPaymentKey(request.paymentKey()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        Participation participation = pendingPayment.getParticipation();
        if (request.amount().compareTo(participation.getFinalAmount()) != 0) {
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
                paymentWriter.save(pendingPayment);
                tossPaymentClient.cancel(pendingPayment.getTossPaymentKey(), "좌석 선점 시간 만료로 인한 자동 취소");
                pendingPayment.completeRefund();
                paymentWriter.save(pendingPayment);
                participationService.cancelByPaymentFailure(pendingPayment.getPaymentId());
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
            log.warn("결제 완료 알림 발송 실패 (paymentId={})", pendingPayment.getPaymentId());
        }
        return PaymentResponse.from(pendingPayment);
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, PaymentRefundRequest request, Long memberId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!payment.getParticipation().getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED|| payment.getStatus() == PaymentStatus.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_REFUNDED);
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        payment.startRefund();
        paymentWriter.save(payment);

        tossPaymentClient.cancel(
                payment.getTossPaymentKey(),
                request.cancelReason()
        );

        payment.completeRefund();
        Payment updatedPayment = paymentWriter.save(payment);

        return PaymentResponse.from(updatedPayment);
    }

    @Transactional
    public void refundByParticipationId(Long participationId) {
        List<Payment> payments = paymentRepository.findByParticipation_ParticipationId(participationId);
        if(payments.isEmpty()){
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        for(Payment payment : payments){
            if (payment.getStatus() != PaymentStatus.PAID) {
                continue;
            }
            payment.startRefund();
            paymentWriter.save(payment);

            tossPaymentClient.cancel(payment.getTossPaymentKey(), "참여 취소로 인한 환불");

            payment.completeRefund();
            paymentWriter.save(payment);
        }

    }

    @Transactional
    public PaymentPrepareResponse prepare(Long participationId, Long memberId) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        if (!participation.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }

        List<Payment> existingPendings = paymentRepository.findAllByParticipation_ParticipationIdAndStatus(participationId,
                PaymentStatus.PENDING);
        paymentRepository.deleteAll(existingPendings);

        String orderId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .participation(participation)
                .orderId(orderId)
                .amount(participation.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now(clock))
                .build();
        paymentWriter.save(payment);

        return new PaymentPrepareResponse(orderId);
    }

    @Transactional
    public void expirePayment(Payment payment) {
        payment.expire();
        paymentWriter.save(payment);
        participationService.cancelByPaymentFailure(payment.getPaymentId());
    }
}
