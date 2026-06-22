package com.back.team9.moyeota.domain.payment.scheduler;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingPaymentGcSchedulerTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;
    @Mock private Clock clock;

    @InjectMocks
    private PendingPaymentGcScheduler scheduler;

    private final Instant fixedInstant = Instant.parse("2026-06-18T10:00:00Z");
    private final ZoneId zone = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setupClock() {
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(zone);
    }

    private Payment pendingPayment(Long paymentId) {
        return Payment.builder()
                .paymentId(paymentId)
                .orderId("order-" + paymentId)
                .amount(new BigDecimal("50000"))
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now(clock).minusMinutes(35))
                .build();
    }

    @Test
    @DisplayName("GC 스케줄러 - PENDING 결제 존재 시 expirePayment 호출")
    void expireStuckPendingPayments_정상케이스_expirePayment호출() {
        Payment payment = pendingPayment(1L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment));

        scheduler.expireStuckPendingPayments();

        verify(paymentService).expirePayment(payment);
    }

    @Test
    @DisplayName("GC 스케줄러 - 여러 PENDING 결제 존재 시 각각 expirePayment 호출")
    void expireStuckPendingPayments_복수건_각각처리() {
        Payment payment1 = pendingPayment(1L);
        Payment payment2 = pendingPayment(2L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment1, payment2));

        scheduler.expireStuckPendingPayments();

        verify(paymentService).expirePayment(payment1);
        verify(paymentService).expirePayment(payment2);
    }

    @Test
    @DisplayName("GC 스케줄러 - 대상 없을 시 아무 처리 없이 종료")
    void expireStuckPendingPayments_대상없음_스킵() {
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of());

        scheduler.expireStuckPendingPayments();

        verify(paymentService, never()).expirePayment(any());
    }

    @Test
    @DisplayName("GC 스케줄러 - 개별 결제 처리 실패 시 로그 후 나머지 건 계속 처리")
    void expireStuckPendingPayments_개별실패_나머지건계속처리() {
        Payment payment1 = pendingPayment(1L);
        Payment payment2 = pendingPayment(2L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment1, payment2));
        willThrow(new RuntimeException("DB 오류"))
                .given(paymentService).expirePayment(payment1);

        scheduler.expireStuckPendingPayments();

        verify(paymentService).expirePayment(payment1);
        verify(paymentService).expirePayment(payment2);
    }

    @Test
    @DisplayName("GC 스케줄러 - 30분 기준 threshold로 조회함")
    void expireStuckPendingPayments_30분기준threshold조회() {
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of());

        scheduler.expireStuckPendingPayments();

        LocalDateTime expectedThreshold = LocalDateTime.now(clock).minusMinutes(30);
        verify(paymentRepository).findAllByStatusAndCreatedAtBefore(
                eq(PaymentStatus.PENDING),
                eq(expectedThreshold)
        );
    }
}
