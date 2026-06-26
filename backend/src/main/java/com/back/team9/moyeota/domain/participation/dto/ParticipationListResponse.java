package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

// 참여자 목록 응답 DTO
public record ParticipationListResponse(
        Long participationId,
        String memberNickname,
        ParticipationStatus status,
        ParticipationPaymentStatus paymentStatus,
        String outboundSeatNumber,
        String returnSeatNumber
) {

    public static ParticipationListResponse from(Participation participation) {
        return new ParticipationListResponse(
                participation.getParticipationId(),
                participation.getMember().getNickname(),
                participation.getStatus(),
                participation.getPaymentStatus(),
                participation.getOutboundSeat().getSeatNumber(),

                // 편도인 경우 null
                participation.getReturnSeat() != null
                        ? participation.getReturnSeat().getSeatNumber()
                        : null
        );
    }
}
