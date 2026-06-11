package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;

import java.time.LocalDate;

public record FundingDetailResponse(
        Long fundingId,
        String title,
        String content,
        LocalDate departureDate,
        FundingStatus status,
        BusType busType,
        Integer minParticipants,
        Integer maxParticipants,
        TripType tripType
) {
}
