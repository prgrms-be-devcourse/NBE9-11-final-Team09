package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

import java.time.LocalDateTime;

public record FundingCreateResponse(
        Long fundingId,
        FundingStatus status,
        LocalDateTime createdAt
) {
}
