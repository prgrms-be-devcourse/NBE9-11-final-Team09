package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.payment.client.TossConfirmResponse;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentWriter paymentWriter;
    private final ParticipationRepository participationRepository;


    @Transactional
    public PaymentResponse confirmDeposit(PaymentConfirmRequest request) {
        return confirmPayment(request, PaymentType.DEPOSIT);
    }

    @Transactional
    public PaymentResponse confirmBalance(PaymentConfirmRequest request) {
        return confirmPayment(request, PaymentType.BALANCE);
    }

    private PaymentResponse confirmPayment(PaymentConfirmRequest request, PaymentType paymentType) {

        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }
        if (paymentRepository.findByTossPaymentKey(request.paymentKey()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        Participation participation = participationRepository.findById(request.participationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));

        if (request.amount().compareTo(new BigDecimal(participation.getFinalAmount())) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        TossConfirmResponse tossResponse = tossPaymentClient.confirm(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        Payment payment = request.toEntity(participation, tossResponse.paymentKey(), PaymentStatus.PAID, paymentType);
        Payment savePayment = paymentWriter.save(payment);

        return PaymentResponse.from(savePayment);
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, PaymentRefundRequest request, Long memberId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!payment.getParticipation().getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.ALREADY_REFUNDED);
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        tossPaymentClient.cancel(
                payment.getTossPaymentKey(),
                request.cancelReason()
        );

        Payment updatedPayment = paymentWriter.update(payment, PaymentStatus.REFUNDED);
        return PaymentResponse.from(updatedPayment);
    }
}
