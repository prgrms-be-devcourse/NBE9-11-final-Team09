package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

// 내 참여 내역 응답 DTO
public record MyParticipationResponse(
        Long participationId,                    // 참여 ID
        String fundingTitle,                     // 모집글 제목
        String routeInfo,                        // 노선 정보 (출발지 → 도착지)
        String outboundSeatNumber,               // 가는편 좌석 번호
        String returnSeatNumber,                 // 오는편 좌석 번호 (편도면 null)
        ParticipationStatus status,              // 참여 상태 (ACTIVE/CANCELED/COMPLETED)
        ParticipationPaymentStatus paymentStatus, // 결제 상태
        boolean canBoard                         // 탑승 가능 여부
) {

    public static MyParticipationResponse from(Participation participation) {
        // 왕복이어도 마이페이지에는 가는편 노선만 표시
        String routeInfo = participation.getOutboundSeat().getPathinfo().getDepartureAddress()
                + " → "
                + participation.getOutboundSeat().getPathinfo().getArrivalAddress();

        // 오는편 좌석 번호 (편도면 null)
        String returnSeatNumber = participation.getReturnSeat() != null
                ? participation.getReturnSeat().getSeatNumber()
                : null;

        // 탑승 가능 여부
        boolean canBoard =
                participation.getStatus() == ParticipationStatus.ACTIVE
                        && participation.getPaymentStatus() == ParticipationPaymentStatus.COMPLETED;

        return new MyParticipationResponse(
                participation.getParticipationId(),
                participation.getFunding().getTitle(),
                routeInfo,
                participation.getOutboundSeat().getSeatNumber(),
                returnSeatNumber,
                participation.getStatus(),
                participation.getPaymentStatus(),
                canBoard
        );
    }
}