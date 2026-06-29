package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.event.NotificationCreatedEvent;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAsyncService {

    private final NotificationRepository notificationRepository;
    private final MailService mailService;
    private final Clock clock;

    @Async("notificationExecutor")
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMail(NotificationCreatedEvent event) {

        Notification notification =
                notificationRepository.findByIdWithMember(
                        event.getNotificationId()
                ).orElseThrow(
                        () -> new BusinessException(
                                ErrorCode.NOTIFICATION_SEND_FAILED
                        )
                );

        try {

            sendMailWithRetry(
                    notification.getMember().getEmail(),
                    notification.getTitle(),
                    notification.getContent()
            );

            notification.markSuccess(LocalDateTime.now(clock));

        } catch (Exception e) {

            notification.markFailed();

            log.error(
                    "메일 발송 실패 notificationId={}",
                    event.getNotificationId(),
                    e
            );
        }
    }

    private void sendMailWithRetry(
            String email,
            String title,
            String content
    ) {

        int retryCount = 0;

        while (retryCount < 3) {

            try {

                mailService.send(email, title, content);
                return;

            } catch (Exception e) {

                retryCount++;

                log.warn(
                        "메일 발송 실패 ({}/3)",
                        retryCount
                );

                if (retryCount >= 3) {
                    throw e;
                }

                try {

                    long delay =
                            (long) Math.pow(2, retryCount) * 1000;

                    Thread.sleep(delay);

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(ex);
                }
            }
        }
    }
}