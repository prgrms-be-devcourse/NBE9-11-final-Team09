package com.back.team9.moyeota.domain.admin.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminFundingListResponse(
        Long fundingId,
        Long memberId,
        String hostEmail,
        String title,
        String content,
        LocalDate departureDate,
        BusType busType,
        FundingStatus status,
        Integer minParticipants,
        Integer maxParticipants,
        Long currentParticipants,
        LocalDateTime createdAt
) {
}