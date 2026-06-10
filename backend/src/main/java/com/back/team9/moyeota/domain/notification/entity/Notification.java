package com.back.team9.moyeota.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long fundingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime emailSentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SendStatus sendStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
