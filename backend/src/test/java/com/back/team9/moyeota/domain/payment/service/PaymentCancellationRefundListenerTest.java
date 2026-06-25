package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCancellationRefundListenerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PaymentCancellationRefundListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "adminEmail", "admin@test.com");
    }

    @Test
    @DisplayName("참여 취소 이벤트 - 첫 번째 시도 성공 시 1회만 호출")
    void handleParticipationCancelled_정상환불성공_1회호출() {
        ParticipationCancelledEvent event = new ParticipationCancelledEvent(1L);

        listener.handleParticipationCancelled(event);

        verify(paymentService, times(1)).refundByParticipationId(1L);
    }

    @Test
    @DisplayName("참여 취소 이벤트 - 일시적 오류 1회 후 성공 시 2회 호출")
    void handleParticipationCancelled_일시적오류1회후성공_2회호출() {
        ParticipationCancelledEvent event = new ParticipationCancelledEvent(1L);
        willThrow(new RuntimeException("network error"))
                .willDoNothing()
                .given(paymentService).refundByParticipationId(1L);

        listener.handleParticipationCancelled(event);

        verify(paymentService, times(2)).refundByParticipationId(1L);
    }

    @Test
    @DisplayName("참여 취소 이벤트 - 일시적 오류 3회 모두 실패 시 3회 호출 후 종료")
    void handleParticipationCancelled_일시적오류3회모두실패_3회호출() {
        ParticipationCancelledEvent event = new ParticipationCancelledEvent(1L);
        willThrow(new RuntimeException("network error"))
                .given(paymentService).refundByParticipationId(1L);

        listener.handleParticipationCancelled(event);

        verify(paymentService, times(3)).refundByParticipationId(1L);
    }

    @Test
    @DisplayName("참여 취소 이벤트 - BusinessException(ORDER_NOT_FOUND) 발생 시 재시도 없이 1회 호출 후 종료")
    void handleParticipationCancelled_BusinessException_재시도없이1회호출() {
        ParticipationCancelledEvent event = new ParticipationCancelledEvent(1L);
        willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND))
                .given(paymentService).refundByParticipationId(1L);

        listener.handleParticipationCancelled(event);

        verify(paymentService, times(1)).refundByParticipationId(1L);
    }

    @Test
    @DisplayName("참여 취소 이벤트 - REFUND_FAILED 발생 시 3회 재시도 후 어드민 알림 발송")
    void handleParticipationCancelled_REFUND_FAILED_3회재시도후_어드민알림발송() {
        ParticipationCancelledEvent event = new ParticipationCancelledEvent(1L);
        willThrow(new BusinessException(ErrorCode.REFUND_FAILED))
                .given(paymentService).refundByParticipationId(1L);

        listener.handleParticipationCancelled(event);

        verify(paymentService, times(3)).refundByParticipationId(1L);
        verify(mailService).send(anyString(), anyString(), anyString());
    }
}
