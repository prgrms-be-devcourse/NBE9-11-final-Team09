package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancellationRefundListener {
    private final PaymentService paymentService;
    private final MailService mailService;
    private final NotificationService notificationService;
    private final ParticipationRepository participationRepository;
    private static final int MAX_RETRY = 3;

    @Value("${admin.email}")
    private String adminEmail;

    @Async("refundExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipationCancelled(ParticipationCancelledEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                paymentService.refundByParticipationId(event.participationId());
                sendRefundCompletedNotification(event.participationId());
                return;
            } catch (Exception e) {
                if (e instanceof BusinessException be && be.getErrorCode() != ErrorCode.REFUND_FAILED) {
                    log.error("환불 처리 실패 - 재시도 불필요 ({}), participationId: {}",
                            be.getErrorCode(), event.participationId());
                    return;
                }
                log.warn("환불 처리 실패 (시도 {}/{}), participationId: {}",
                        attempt, MAX_RETRY, event.participationId(), e);
                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        log.error("환불 처리 최종 실패 — 수동 처리 필요. participationId: {}", event.participationId());

        try {
            mailService.send(
                    adminEmail,
                    "[긴급] 환불 최종 실패 — 수동 처리 필요",
                    "participationId: " + event.participationId() + " 환불이 3회 재시도 후 최종 실패했습니다. 수동 처리가 필요합니다."
            );
        } catch (Exception mailEx) {
            log.error("어드민 알림 발송 실패: {}", mailEx.getMessage(), mailEx);
        }
    }

    private void sendRefundCompletedNotification(Long participationId) {
        try {
            Participation participation = participationRepository.findById(participationId).orElse(null);
            if (participation == null) return;
            notificationService.sendMimeMessage(
                    participation.getMember().getMemberId(),
                    participation.getFunding().getFundingId(),
                    NotificationType.REFUND_COMPLETED
            );
        } catch (Exception e) {
            log.warn("환불 완료 알림 발송 실패 — participationId={}", participationId, e);
        }
    }
}
