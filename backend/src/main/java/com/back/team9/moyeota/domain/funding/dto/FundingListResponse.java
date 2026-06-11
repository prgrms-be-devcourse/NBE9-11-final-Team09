package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

import java.time.LocalDateTime;

public record FundingListResponse(
        Long fundingId,
        String title,
        String hostNickname,
        String departureAddress,
        String arrivalAddress,
        LocalDateTime departureTime,
        FundingStatus status,
        Integer currentParticipants,
        Integer minParticipants,
        Integer maxParticipants
) {
}
