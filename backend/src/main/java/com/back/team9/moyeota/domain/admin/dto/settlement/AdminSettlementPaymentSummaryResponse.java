package com.back.team9.moyeota.domain.admin.dto.settlement;

public record AdminSettlementPaymentSummaryResponse(
        Long totalPaidCount,
        Long depositPaidCount,
        Long balancePaidCount,
        Long totalPaidAmount
) {
}
