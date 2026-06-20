package com.back.team9.moyeota.domain.payment.scheduler;

import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.payment.service.PaymentWriter;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PendingPaymentGcSchedulerTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentWriter paymentWriter;
    @Mock private Clock clock;
    @Mock private ParticipationService participationService;

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
    @DisplayName("GC 스케줄러 - PENDING 결제 존재 시 FAILED 전환 및 참여 취소 호출")
    void expireStuckPendingPayments_정상케이스_FAILED전환후참여취소() {
        Payment payment = pendingPayment(1L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment));

        scheduler.expireStuckPendingPayments();

        verify(paymentWriter).save(payment);
        verify(participationService).cancelByPaymentFailure(1L);
    }

    @Test
    @DisplayName("GC 스케줄러 - 여러 PENDING 결제 존재 시 각각 처리")
    void expireStuckPendingPayments_복수건_각각처리() {
        Payment payment1 = pendingPayment(1L);
        Payment payment2 = pendingPayment(2L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment1, payment2));

        scheduler.expireStuckPendingPayments();

        verify(paymentWriter).save(payment1);
        verify(paymentWriter).save(payment2);
        verify(participationService).cancelByPaymentFailure(1L);
        verify(participationService).cancelByPaymentFailure(2L);
    }

    @Test
    @DisplayName("GC 스케줄러 - 대상 없을 시 아무 처리 없이 종료")
    void expireStuckPendingPayments_대상없음_스킵() {
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of());

        scheduler.expireStuckPendingPayments();

        verify(paymentWriter, never()).save(any());
        verify(participationService, never()).cancelByPaymentFailure(anyLong());
    }

    @Test
    @DisplayName("GC 스케줄러 - 개별 결제 처리 실패 시 로그 후 나머지 건 계속 처리")
    void expireStuckPendingPayments_개별실패_나머지건계속처리() {
        Payment payment1 = pendingPayment(1L);
        Payment payment2 = pendingPayment(2L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment1, payment2));
        willThrow(new RuntimeException("DB 오류"))
                .given(paymentWriter).save(payment1);

        scheduler.expireStuckPendingPayments();

        verify(paymentWriter).save(payment1);
        verify(participationService, never()).cancelByPaymentFailure(1L);

        verify(paymentWriter).save(payment2);
        verify(participationService).cancelByPaymentFailure(2L);
    }

    @Test
    @DisplayName("GC 스케줄러 - expire() 후 status가 FAILED로 변경됨")
    void expireStuckPendingPayments_expire호출후FAILED상태() {
        Payment payment = pendingPayment(1L);
        given(paymentRepository.findAllByStatusAndCreatedAtBefore(eq(PaymentStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(payment));

        scheduler.expireStuckPendingPayments();

        verify(paymentWriter).save(payment);
        assert payment.getStatus() == PaymentStatus.FAILED;
        assert payment.getUpdatedAt() != null;
    }
}
