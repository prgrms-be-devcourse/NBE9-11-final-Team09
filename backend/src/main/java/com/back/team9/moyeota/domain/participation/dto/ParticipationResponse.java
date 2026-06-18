package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 참여 응답 DTO
public record ParticipationResponse(
        Long participationId, //참여 ID (취소 API에서 사용)
        ParticipationStatus status, //참여 상태 (ACTIVE/CANCELED/COMPLETED)
        ParticipationPaymentStatus paymentStatus, //결제 상태 (ACTIVE/COMPLETED/CANCELED/NO_SHOW)
        BigDecimal finalAmount, //최종 확정 금액
        Long outboundSeatId, //좌석 정보
        Long returnSeatId, //좌석 정보 (returnSeatId는 null 가능)
        LocalDateTime createdAt //참여 신청 시각
) {

    // Entity → DTO 변환
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
