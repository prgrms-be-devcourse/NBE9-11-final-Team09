package com.back.team9.moyeota.domain.member.dto;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;

import java.time.LocalDateTime;

public record MemberPaymentResponse(
        Long paymentId,
        String fundingTitle,
        PaymentType type,
        Integer amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {
    public static MemberPaymentResponse from(Payment payment) {
        return new MemberPaymentResponse(
                payment.getPaymentId(),
                payment.getParticipation().getFunding().getTitle(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}