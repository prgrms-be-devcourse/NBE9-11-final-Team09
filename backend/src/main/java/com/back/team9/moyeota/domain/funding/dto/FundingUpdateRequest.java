package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoUpdateRequest;

import java.util.List;

public record FundingUpdateRequest(
        String title,
        String content,
        BusType busType,
        Integer minParticipants,
        Integer maxParticipants,
        TripType tripType,
        Integer totalPrice,
        List<PathInfoUpdateRequest> paths
) {
}
