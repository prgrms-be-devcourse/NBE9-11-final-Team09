package com.back.team9.moyeota.domain.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SettlementCreateRequest(
        @NotNull Long fundingId,
        @NotNull @Positive Integer totalAmount
        ) {
}
