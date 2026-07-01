package com.back.team9.moyeota.domain.admin.dto.statistics;

public record AdminStatisticsResponse(
        long totalUsers,
        long activeUsers,
        long withdrawnUsers,
        long activeFundings,
        long completedFundings,
        long cancelledFundings,
        long totalPaymentAmount,
        long pendingSettlements,
        long pendingReports
) {
}
