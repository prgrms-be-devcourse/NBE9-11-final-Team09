package com.back.team9.moyeota.domain.member.dto;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberParticipationResponse(
        Long participationId,
        Long fundingId,
        String fundingTitle,
        LocalDate departureDate,
        ParticipationStatus status,
        ParticipationPaymentStatus paymentStatus,
        LocalDateTime createdAt
) {
    public static MemberParticipationResponse from(
            Participation participation
    ) {
        return new MemberParticipationResponse(
                participation.getParticipationId(),
                participation.getFunding().getFundingId(),
                participation.getFunding().getTitle(),
                participation.getFunding().getDepartureDate(),
                participation.getStatus(),
                participation.getPaymentStatus(),
                participation.getCreatedAt()
        );
    }
}