package com.back.team9.moyeota.domain.admin.dto.settlement;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminSettlementDetailResponse(
        Long settlementId,
        Long memberId,
        String hostEmail,
        String hostNickname,
        Long fundingId,
        String fundingTitle,
        FundingStatus fundingStatus,
        LocalDate departureDate,
        BigDecimal totalAmount,
        BigDecimal platformFee,
        BigDecimal hostPaybackAmount,
        SettlementStatus status,
        LocalDateTime paybackPaidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        AdminSettlementPaymentSummaryResponse paymentSummary
) {
    public static AdminSettlementDetailResponse of(
            Settlement settlement,
            AdminSettlementPaymentSummaryResponse paymentSummary
    ) {
        return new AdminSettlementDetailResponse(
                settlement.getSettlementId(),
                settlement.getMember().getMemberId(),
                settlement.getMember().getEmail(),
                settlement.getMember().getNickname(),
                settlement.getFunding().getFundingId(),
                settlement.getFunding().getTitle(),
                settlement.getFunding().getStatus(),
                settlement.getFunding().getDepartureDate(),
                settlement.getTotalAmount(),
                settlement.getPlatformFee(),
                settlement.getHostPaybackAmount(),
                settlement.getStatus(),
                settlement.getPaybackPaidAt(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt(),
                paymentSummary
        );
    }
}
