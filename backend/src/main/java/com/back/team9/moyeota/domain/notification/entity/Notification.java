package com.back.team9.moyeota.domain.notification.entity;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private Funding funding;

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

    public void markSuccess(LocalDateTime emailSentAt) {
        this.sendStatus = SendStatus.SUCCESS;
        this.emailSentAt = emailSentAt;
    }

    public void markFailed() {
        this.sendStatus = SendStatus.FAILED;
    }
}
