package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 참여 응답 DTO
public record ParticipationResponse(
        Long participationId,
        ParticipationStatus status,
        ParticipationPaymentStatus paymentStatus,
        BigDecimal finalAmount,
        Long outboundSeatId,
        Long returnSeatId,
        LocalDateTime createdAt
) {

    public static ParticipationResponse from(Participation participation) {

        // 편도인 경우 null
        Long returnSeatId = participation.getReturnSeat() == null
                ? null
                : participation.getReturnSeat().getSeatId();

        return new ParticipationResponse(
                participation.getParticipationId(),
                participation.getStatus(),
                participation.getPaymentStatus(),
                participation.getFinalAmount(),
                participation.getOutboundSeat().getSeatId(),
                returnSeatId,
                participation.getCreatedAt()
        );
    }
}
