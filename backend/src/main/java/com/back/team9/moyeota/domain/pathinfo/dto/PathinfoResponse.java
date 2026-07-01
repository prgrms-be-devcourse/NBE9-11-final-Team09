package com.back.team9.moyeota.domain.pathinfo.dto;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;

import java.time.LocalDateTime;

public record PathinfoResponse(
        Long pathinfoId,
        LocalDateTime departureTime,
        String departureAddress,
        Region departureRegion,
        String arrivalAddress,
        Region arrivalRegion,
        PathinfoStatus status,
        Direction direction
) {
    public static PathinfoResponse from(Pathinfo pathinfo) {
        return new PathinfoResponse(
                pathinfo.getPathinfoId(),
                pathinfo.getDepartureTime(),
                pathinfo.getDepartureAddress(),
                pathinfo.getDepartureRegion(),
                pathinfo.getArrivalAddress(),
                pathinfo.getArrivalRegion(),
                pathinfo.getStatus(),
                pathinfo.getDirection()
        );
    }
}
