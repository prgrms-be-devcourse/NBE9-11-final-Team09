package com.back.team9.moyeota.domain.notification.dto;

import com.back.team9.moyeota.domain.notification.entity.NotificationType;

public record TestRequest(
        Long fundingId,
        NotificationType type
) {}
