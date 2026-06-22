package com.back.team9.moyeota.domain.payment.scheduler;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentGcScheduler {

    private final PaymentRepository paymentRepository;
    private final Clock clock;
    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000) // 5분
    public void expireStuckPendingPayments() {
        LocalDateTime threshold = LocalDateTime.now(clock).minusMinutes(30);
        List<Payment> stuckPayments = paymentRepository
                .findAllByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold);

        if (stuckPayments.isEmpty()) return;

        log.info("PENDING GC 시작 — 대상: {}건", stuckPayments.size());
        for (Payment payment : stuckPayments) {
            try {
                paymentService.expirePayment(payment);
            } catch (Exception e) {
                log.error("PENDING GC 실패 — paymentId={}", payment.getPaymentId(), e);
            }
        }
        log.info("PENDING GC 완료");
    }

}
