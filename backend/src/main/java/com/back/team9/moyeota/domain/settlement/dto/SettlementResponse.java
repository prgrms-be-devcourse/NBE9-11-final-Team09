package com.back.team9.moyeota.domain.settlement.dto;

import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(
        Long settlementId,
        BigDecimal totalAmount,
        BigDecimal platformFee,
        BigDecimal hostPaybackAmount,
        SettlementStatus status,
        Boolean paybackHold,
        LocalDateTime paybackPaidAt,
        LocalDateTime createdAt
) {
    public static SettlementResponse from(Settlement settlement){
        return new SettlementResponse(
                settlement.getSettlementId(),
                settlement.getTotalAmount(),
                settlement.getPlatformFee(),
                settlement.getHostPaybackAmount(),
                settlement.getStatus(),
                settlement.getPaybackHold(),
                settlement.getPaybackPaidAt(),
                settlement.getCreatedAt()
        );
    }
}
