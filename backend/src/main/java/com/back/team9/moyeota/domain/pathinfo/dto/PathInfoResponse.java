package com.back.team9.moyeota.domain.pathinfo.dto;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;

import java.time.LocalDateTime;

public record PathInfoResponse(
        Long pathInfoId,
        LocalDateTime departureTime,
        String departureAddress,
        Region departureRegion,
        String arrivalAddress,
        Region arrivalRegion,
        PathInfoStatus status,
        Direction direction
) {
    public static PathInfoResponse from(PathInfo pathInfo) {
        return new PathInfoResponse(
                pathInfo.getPathInfoId(),
                pathInfo.getDepartureTime(),
                pathInfo.getDepartureAddress(),
                pathInfo.getDepartureRegion(),
                pathInfo.getArrivalAddress(),
                pathInfo.getArrivalRegion(),
                pathInfo.getStatus(),
                pathInfo.getDirection()
        );
    }
}
