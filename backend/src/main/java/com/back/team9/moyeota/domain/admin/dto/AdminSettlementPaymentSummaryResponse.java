package com.back.team9.moyeota.domain.admin.dto;

public record AdminSettlementPaymentSummaryResponse(
        Long totalPaidCount,
        Long depositPaidCount,
        Long balancePaidCount,
        Long totalPaidAmount
) {
}