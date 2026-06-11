package com.back.team9.moyeota.domain.payment.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PaymentConfirmRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @NotNull Integer amount,
        @NotNull Long participationId
        ) {

    public Payment toEntity(Participation participation, String tossPaymentKey, PaymentStatus status) {
        return Payment.builder()
                .participation(participation)
                .paymentType(PaymentType.DEPOSIT)
                .amount(this.amount)
                .tossPaymentKey(tossPaymentKey)
                .orderId(this.orderId)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
