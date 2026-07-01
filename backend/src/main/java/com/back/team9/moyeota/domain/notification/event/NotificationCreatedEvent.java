package com.back.team9.moyeota.domain.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationCreatedEvent {

    private final Long notificationId;
}
