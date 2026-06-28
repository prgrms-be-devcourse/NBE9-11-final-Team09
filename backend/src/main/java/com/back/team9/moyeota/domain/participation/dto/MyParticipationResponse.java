package com.back.team9.moyeota.domain.participation.dto;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.policy.FundingPricePolicy;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record MyParticipationResponse(
        Long participationId,
        Long fundingId,
        String fundingTitle,
        String routeInfo,
        String outboundSeatNumber,
        String returnSeatNumber,
        ParticipationStatus status,
        ParticipationPaymentStatus paymentStatus,
        boolean canBoard,
        LocalDateTime departureTime,
        long balanceAmount
) {

    public static MyParticipationResponse from(Participation participation) {
        String routeInfo = participation.getOutboundSeat().getPathinfo().getDepartureAddress()
                + " → "
                + participation.getOutboundSeat().getPathinfo().getArrivalAddress();

        String returnSeatNumber = participation.getReturnSeat() != null
                ? participation.getReturnSeat().getSeatNumber()
                : null;

        boolean canBoard =
                participation.getStatus() == ParticipationStatus.ACTIVE
                        && participation.getPaymentStatus() == ParticipationPaymentStatus.COMPLETED;

        Funding funding = participation.getFunding();
        BigDecimal deposit = FundingPricePolicy.calculateRoundedPrice(
                        funding.getTotalPrice(),
                        funding.getMaxParticipants() + 1
                )
                .divide(BigDecimal.valueOf(2), 0, RoundingMode.CEILING);

        BigDecimal finalAmt = participation.getFinalAmount();
        long balanceAmount = (finalAmt != null && finalAmt.compareTo(BigDecimal.ZERO) > 0)
                ? finalAmt.subtract(deposit).longValue()
                : 0L;

        return new MyParticipationResponse(
                participation.getParticipationId(),
                funding.getFundingId(),
                funding.getTitle(),
                routeInfo,
                participation.getOutboundSeat().getSeatNumber(),
                returnSeatNumber,
                participation.getStatus(),
                participation.getPaymentStatus(),
                canBoard,
                participation.getOutboundSeat().getPathinfo().getDepartureTime(),
                balanceAmount
        );
    }
}
