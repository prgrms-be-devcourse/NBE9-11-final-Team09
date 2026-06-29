package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.entity.SendStatus;
import com.back.team9.moyeota.domain.notification.event.NotificationCreatedEvent;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationTransactionService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationTemplateService templateService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAndPublish(Member member, Funding funding, NotificationType type) {
        String title = templateService.getSubject(type, funding.getTitle());
        String content = templateService.buildContent(
                type, member.getNickname(), funding.getTitle());

        Notification notification = Notification.builder()
                .member(member)
                .funding(funding)
                .notificationType(type)
                .title(title)
                .content(content)
                .sendStatus(SendStatus.PENDING)
                .build();

        Notification saved = notificationRepository.save(notification);

        // 이 내부 트랜잭션이 커밋된 후 AFTER_COMMIT 실행
        // → 비동기 리스너가 DB에서 항상 조회 성공
        eventPublisher.publishEvent(
                new NotificationCreatedEvent(saved.getNotificationId()));
    }
}
