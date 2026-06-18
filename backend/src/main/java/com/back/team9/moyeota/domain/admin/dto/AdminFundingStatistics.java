package com.back.team9.moyeota.domain.admin.dto;

public record AdminFundingStatistics(
        Long activeFundings,
        Long completedFundings,
        Long cancelledFundings
) {
}