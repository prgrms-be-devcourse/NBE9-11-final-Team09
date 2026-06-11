package com.back.team9.moyeota.domain.pathinfo.dto;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;

import java.time.LocalDateTime;

public record PathInfoUpdateRequest(
        LocalDateTime departureTime,
        String departureAddress,
        Region departureRegion,
        String arrivalAddress,
        Region arrivalRegion,
        Direction direction
) {
}
