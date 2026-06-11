package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;

public record FundingCreateRequest(
        String title,
        String content,
        BusType busType,
        Integer minParticipants,
        Integer maxParticipants,
        TripType tripType
        //,        List<PathCreateRequest> paths
) {
}
