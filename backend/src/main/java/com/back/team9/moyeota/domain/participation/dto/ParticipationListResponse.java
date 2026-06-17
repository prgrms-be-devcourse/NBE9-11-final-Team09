package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

// 참여자 목록 응답 DTO
public record ParticipationListResponse(
        Long participationId, //참여 ID
        String memberNickname, //참여자 닉네임 (방장이 누가 참여했는지 식별용)
        ParticipationStatus status, //참여 상태
        ParticipationPaymentStatus paymentStatus, //결제 상태
        String outboundSeatNumber, //좌석 번호 (좌석ID보다 의미있는 정보)
        String returnSeatNumber //좌석 번호 (좌석ID보다 의미있는 정보)
) {

    // Entity → DTO 변환
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
