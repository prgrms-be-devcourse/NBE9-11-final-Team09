package com.back.team9.moyeota.domain.settlement.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SettlementCreateRequest(
        @NotNull Long fundingId,
        @NotNull @Positive @Digits(integer = 10, fraction = 0) BigDecimal totalAmount
        ) {
}
