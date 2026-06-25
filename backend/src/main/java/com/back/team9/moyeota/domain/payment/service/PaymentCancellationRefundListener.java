package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
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
    private static final int MAX_RETRY = 3;

    @Value("${admin.email}")
    private String adminEmail;

    @Async("refundExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipationCancelled(ParticipationCancelledEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                paymentService.refundByParticipationId(event.participationId());
                return;
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.REFUND_FAILED) {
                    throw new RuntimeException("Toss 환불 API 실패 (재시도 대상)", e);
                }
                log.error("환불 처리 실패 - 재시도 불필요 ({}), participationId: {}",
                        e.getErrorCode(), event.participationId());
                return;
            } catch (Exception e) {
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
}
