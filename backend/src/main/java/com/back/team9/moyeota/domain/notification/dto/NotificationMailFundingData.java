package com.back.team9.moyeota.domain.notification.dto;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

import java.time.LocalDateTime;

public record NotificationMailFundingData(
        String nickname,
        String fundingTitle,
        String title,
        String hostNickname,
        String departureAddress,
        String arrivalAddress,
        LocalDateTime departureTime,
        Integer totalPrice
) {}
