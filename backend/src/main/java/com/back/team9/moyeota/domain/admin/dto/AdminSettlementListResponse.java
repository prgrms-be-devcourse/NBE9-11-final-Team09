package com.back.team9.moyeota.domain.admin.dto;

import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;

import java.time.LocalDateTime;

public record AdminSettlementListResponse(
        Long settlementId,
        Long memberId,
        String hostEmail,
        Long fundingId,
        String fundingTitle,
        Integer totalAmount,
        Integer platformFee,
        Integer hostPaybackAmount,
        SettlementStatus status,
        LocalDateTime paybackPaidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminSettlementListResponse from(Settlement settlement) {
        return new AdminSettlementListResponse(
                settlement.getSettlementId(),
                settlement.getMember().getMemberId(),
                settlement.getMember().getEmail(),
                settlement.getFunding().getFundingId(),
                settlement.getFunding().getTitle(),
                settlement.getTotalAmount(),
                settlement.getPlatformFee(),
                settlement.getHostPaybackAmount(),
                settlement.getStatus(),
                settlement.getPaybackPaidAt(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }
}