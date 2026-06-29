package com.back.team9.moyeota.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NotificationMailDto {

    private String nickname;

    private String fundingTitle;

    private List<RouteDto> routes;

    private String reason;
    private String message;
    private Integer amount;
    private LocalDateTime paymentTime;
}
