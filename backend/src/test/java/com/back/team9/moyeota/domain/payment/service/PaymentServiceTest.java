package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.payment.client.TossConfirmResponse;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private PaymentWriter paymentWriter;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("보증금 결제 승인 - 정상 결제 성공")
    void confirmDeposit_정상요청_결제성공() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
        );

        TossConfirmResponse tossResponse = new TossConfirmResponse(
                "test_paymentKey", "test_orderId", "DONE", new BigDecimal("50000")
        );

        Payment savedPayment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        given(paymentRepository.findByOrderId("test_orderId")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "test_orderId", new BigDecimal("50000"))).willReturn(tossResponse);
        given(paymentWriter.save(any(Payment.class))).willReturn(savedPayment);

        // When
        PaymentResponse response = paymentService.confirmDeposit(request);

        // Then
        assertThat(response.paymentId()).isEqualTo(1L);
        assertThat(response.paymentType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(response.tossPaymentKey()).isEqualTo("test_paymentKey");
        assertThat(response.orderId()).isEqualTo("test_orderId");
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        verify(paymentWriter).save(any(Payment.class));
    }

    @Test
    @DisplayName("보증금 결제 승인 - 중복 orderId 요청 시 DUPLICATE_PAYMENT 예외 발생")
    void confirmDeposit_중복orderId_DUPLICATE_PAYMENT예외() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
        );

        given(paymentRepository.findByOrderId("test_orderId"))
                .willReturn(Optional.of(Payment.builder()
                        .paymentId(1L)
                        .orderId("test_orderId")
                        .status(PaymentStatus.PAID)
                        .createdAt(LocalDateTime.now())
                        .build()));

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_PAYMENT));

        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("보증금 결제 승인 - 중복 paymentKey 요청 시 DUPLICATE_PAYMENT 예외 발생")
    void confirmDeposit_중복paymentKey_DUPLICATE_PAYMENT예외() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId_new", new BigDecimal("50000"), 1L
        );

        given(paymentRepository.findByOrderId("test_orderId_new")).willReturn(Optional.empty());
        given(paymentRepository.findByTossPaymentKey("test_paymentKey"))
                .willReturn(Optional.of(Payment.builder()
                        .paymentId(1L)
                        .tossPaymentKey("test_paymentKey")
                        .status(PaymentStatus.PAID)
                        .createdAt(LocalDateTime.now())
                        .build()));

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_PAYMENT));

        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
        verify(paymentWriter, never()).save(any());
    }

    //Todo: ParticipationRepository  머지 후 주석 해제 — 금액 일치 시 정상 결제 성공 케이스
//    @Test
//    @DisplayName("보증금 결제 승인 - 참여 금액 일치 시 결제 성공")
//    void confirmDeposit_금액일치_결제성공() {
//        // Given
//        PaymentConfirmRequest request = new PaymentConfirmRequest(
//                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
//        );
//
//        Participation participation = Participation.builder()
//                .participationId(1L)
//                .finalAmount(new BigDecimal("50000"))
//                .build();
//
//        TossConfirmResponse tossResponse = new TossConfirmResponse(
//                "test_paymentKey", "test_orderId", "DONE", new BigDecimal("50000")
//        );
//
//        Payment savedPayment = Payment.builder()
//                .paymentId(1L)
//                .participation(participation)
//                .paymentType(PaymentType.DEPOSIT)
//                .amount(new BigDecimal("50000"))
//                .tossPaymentKey("test_paymentKey")
//                .orderId("test_orderId")
//                .status(PaymentStatus.PAID)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        given(paymentRepository.findByOrderId("test_orderId")).willReturn(Optional.empty());
//        given(participationRepository.findById(1L)).willReturn(Optional.of(participation));
//        given(tossPaymentClient.confirm("test_paymentKey", "test_orderId", new BigDecimal("50000"))).willReturn(tossResponse);
//        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);
//
//        // When
//        PaymentResponse response = paymentService.confirmDeposit(request);
//
//        // Then
//        assertThat(response.participationId()).isEqualTo(1L);
//        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
//        verify(paymentWriter).save(any(Payment.class));
//    }

    //Todo: ParticipationRepository 머지 후 주석 해제 — 금액 불일치 시 PAYMENT_AMOUNT_MISMATCH 예외 케이스
//    @Test
//    @DisplayName("보증금 결제 승인 - 참여 금액 불일치 시 PAYMENT_AMOUNT_MISMATCH 예외 발생")
//    void confirmDeposit_금액불일치_PAYMENT_AMOUNT_MISMATCH예외() {
//        // Given
//        PaymentConfirmRequest request = new PaymentConfirmRequest(
//                "test_paymentKey", "test_orderId", new BigDecimal("99999"), 1L
//        );
//
//        Participation participation = Participation.builder()
//                .participationId(1L)
//                .finalAmount(new BigDecimal("50000"))
//                .build();
//
//        given(paymentRepository.findByOrderId("test_orderId")).willReturn(Optional.empty());
//        given(participationRepository.findById(1L)).willReturn(Optional.of(participation));
//
//        // When & Then
//        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
//                .isInstanceOf(BusinessException.class)
//                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
//                        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));
//
//        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
//        verify(paymentWriter, never()).save(any());
//    }

    @Test
    @DisplayName("잔액 결제 승인 - 정상 결제 성공")
    void confirmBalance_정상요청_결제성공() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
        );

        TossConfirmResponse tossResponse = new TossConfirmResponse(
                "test_paymentKey", "test_orderId", "DONE", new BigDecimal("50000")
        );

        Payment savedPayment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.BALANCE)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        given(paymentRepository.findByOrderId("test_orderId")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "test_orderId", new BigDecimal("50000"))).willReturn(tossResponse);
        given(paymentWriter.save(any(Payment.class))).willReturn(savedPayment);

        // When
        PaymentResponse response = paymentService.confirmBalance(request);

        // Then
        assertThat(response.paymentId()).isEqualTo(1L);
        assertThat(response.paymentType()).isEqualTo(PaymentType.BALANCE);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(response.tossPaymentKey()).isEqualTo("test_paymentKey");
        assertThat(response.orderId()).isEqualTo("test_orderId");
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        verify(paymentWriter).save(any(Payment.class));
    }

    @Test
    @DisplayName("잔액 결제 승인 - 중복 orderId 요청 시 DUPLICATE_PAYMENT 예외 발생")
    void confirmBalance_중복orderId_DUPLICATE_PAYMENT예외() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
        );

        given(paymentRepository.findByOrderId("test_orderId"))
                .willReturn(Optional.of(Payment.builder()
                        .paymentId(1L)
                        .orderId("test_orderId")
                        .status(PaymentStatus.PAID)
                        .createdAt(LocalDateTime.now())
                        .build()));

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmBalance(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_PAYMENT));

        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("잔액 결제 승인 - 토스 API 실패 시 TOSS_PAYMENT_FAILED 예외 발생")
    void confirmBalance_토스API실패_TOSS_PAYMENT_FAILED예외() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
        );

        given(paymentRepository.findByOrderId("test_orderId")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "test_orderId", new BigDecimal("50000")))
                .willThrow(new BusinessException(ErrorCode.TOSS_PAYMENT_FAILED));

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmBalance(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOSS_PAYMENT_FAILED));

        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("보증금 결제 승인 - 토스 API 실패 시 TOSS_PAYMENT_FAILED 예외 발생")
    void confirmDeposit_토스API실패_TOSS_PAYMENT_FAILED예외() {
        // Given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", "test_orderId", new BigDecimal("50000"), 1L
        );

        given(paymentRepository.findByOrderId("test_orderId")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "test_orderId", new BigDecimal("50000")))
                .willThrow(new BusinessException(ErrorCode.TOSS_PAYMENT_FAILED));

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOSS_PAYMENT_FAILED));

        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("환불 - 정상 환불 성공")
    void refund_정상요청_환불성공() {
        // Given
        PaymentRefundRequest request = new PaymentRefundRequest("변심");

        Payment payment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        Payment refundedPayment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(paymentWriter.update(any(Payment.class), eq(PaymentStatus.REFUNDED))).willReturn(refundedPayment);

        // When
        PaymentResponse response = paymentService.refund(1L, request);

        // Then
        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        verify(tossPaymentClient).cancel("test_paymentKey", "변심");
        verify(paymentWriter).update(any(Payment.class), eq(PaymentStatus.REFUNDED));
    }

    @Test
    @DisplayName("환불 - 존재하지 않는 결제 ID 요청 시 ORDER_NOT_FOUND 예외 발생")
    void refund_존재하지않는결제_ORDER_NOT_FOUND예외() {
        // Given
        PaymentRefundRequest request = new PaymentRefundRequest("변심");

        given(paymentRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.refund(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - 이미 환불된 결제 요청 시 ALREADY_REFUNDED 예외 발생")
    void refund_이미환불된결제_ALREADY_REFUNDED예외() {
        // Given
        PaymentRefundRequest request = new PaymentRefundRequest("변심");

        Payment refundedPayment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(refundedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.refund(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_REFUNDED));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - PAID 아닌 다른 상태(FAILED 등) 요청 시 INVALID_PAYMENT_STATUS 예외 발생")
    void refund_FAILED상태결제_INVALID_PAYMENT_STATUS예외() {
        // Given
        PaymentRefundRequest request = new PaymentRefundRequest("변심");

        Payment failedPayment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(failedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.refund(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - 토스 API 실패 시 REFUND_FAILED 예외 발생")
    void refund_토스API실패_REFUND_FAILED예외() {
        // Given
        PaymentRefundRequest request = new PaymentRefundRequest("변심");

        Payment payment = Payment.builder()
                .paymentId(1L)
                .participation(null)
                .paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .tossPaymentKey("test_paymentKey")
                .orderId("test_orderId")
                .status(PaymentStatus.PAID)
                .createdAt(LocalDateTime.now())
                .build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        willThrow(new BusinessException(ErrorCode.REFUND_FAILED))
                .given(tossPaymentClient).cancel(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> paymentService.refund(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REFUND_FAILED));

        verify(paymentWriter, never()).update(any(), any());
    }
}
