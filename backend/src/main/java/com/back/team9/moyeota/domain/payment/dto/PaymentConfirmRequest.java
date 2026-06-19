package com.back.team9.moyeota.domain.payment.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentConfirmRequest(
        @NotBlank String paymentKey,
        @NotNull @Digits(integer = 10, fraction = 0) BigDecimal amount,
        @NotNull Long participationId
        ) {
}
