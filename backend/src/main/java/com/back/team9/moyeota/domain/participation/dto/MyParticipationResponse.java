package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

public record MyParticipationResponse(
        Long participationId,
        String fundingTitle,
        String routeInfo,
        String outboundSeatNumber,
        String returnSeatNumber,
        ParticipationStatus status,
        ParticipationPaymentStatus paymentStatus,
        boolean canBoard
) {

    public static MyParticipationResponse from(Participation participation) {
        // 왕복이어도 마이페이지에는 가는편 노선만 표시
        String routeInfo = participation.getOutboundSeat().getPathinfo().getDepartureAddress()
                + " → "
                + participation.getOutboundSeat().getPathinfo().getArrivalAddress();

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