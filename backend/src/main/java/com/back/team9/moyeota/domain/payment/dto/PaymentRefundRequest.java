package com.back.team9.moyeota.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentRefundRequest(
        @NotBlank String cancelReason
) {
}
