package com.back.team9.moyeota.domain.payment.dto;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long participationId,
        PaymentType paymentType,
        String orderId,
        BigDecimal amount,
        String tossPaymentKey,
        PaymentStatus status,
        LocalDateTime createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getParticipation().getParticipationId(),
                payment.getPaymentType(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getTossPaymentKey(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

}
