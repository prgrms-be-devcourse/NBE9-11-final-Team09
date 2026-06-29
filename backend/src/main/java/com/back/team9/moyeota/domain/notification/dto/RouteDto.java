package com.back.team9.moyeota.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RouteDto {

    private String departure;

    private String arrival;

    private LocalDateTime departureTime;
}
