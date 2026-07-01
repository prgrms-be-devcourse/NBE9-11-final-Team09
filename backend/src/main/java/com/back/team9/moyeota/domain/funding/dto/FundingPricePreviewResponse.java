package com.back.team9.moyeota.domain.funding.dto;

import java.math.BigDecimal;

public record FundingPricePreviewResponse(
        BigDecimal totalPrice
) {
}
