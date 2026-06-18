package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancellationRefundListener {
    private final PaymentService paymentService;
    private static final int MAX_RETRY = 3;

    @Async("refundExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipationCancelled(ParticipationCancelledEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                paymentService.refundByParticipationId(event.participationId());
                return;
            } catch (BusinessException e) {
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
    }
}
