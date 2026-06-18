package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.payment.client.TossConfirmResponse;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentPrepareResponse;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private TossPaymentClient tossPaymentClient;
    @Mock private PaymentWriter paymentWriter;

    @InjectMocks
    private PaymentService paymentService;

    private Participation mockParticipationForConfirm(BigDecimal finalAmount) {
        Participation participation = mock(Participation.class);
        given(participation.getFinalAmount()).willReturn(finalAmount);
        return participation;
    }

    private Participation mockParticipationForPrepare(Long memberId) {
        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(memberId);
        Participation participation = mock(Participation.class);
        given(participation.getMember()).willReturn(member);
        return participation;
    }

    private Participation mockParticipationForRefund(Long memberId) {
        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(memberId);
        Participation participation = mock(Participation.class);
        given(participation.getMember()).willReturn(member);
        return participation;
    }

    private Payment pendingPaymentOf(Participation participation) {
        return Payment.builder()
                .paymentId(1L)
                .participation(participation)
                .orderId("uuid-order-id")
                .amount(new BigDecimal("50000"))
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== prepare =====

    @Test
    @DisplayName("결제 준비 - 정상 요청 시 UUID orderId 반환 및 PENDING Payment 저장")
    void prepare_정상요청_PENDING결제저장() {
        Participation participation = mockParticipationForPrepare(1L);
        given(participationRepository.findById(1L)).willReturn(Optional.of(participation));

        PaymentPrepareResponse response = paymentService.prepare(1L, 1L);

        assertThat(response.orderId()).isNotBlank();
        assertThat(response.orderId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(paymentWriter).save(argThat(p ->
                p.getStatus() == PaymentStatus.PENDING && p.getOrderId() != null
        ));
    }

    @Test
    @DisplayName("결제 준비 - 존재하지 않는 participationId 요청 시 PARTICIPATION_NOT_FOUND 예외")
    void prepare_존재하지않는participationId_PARTICIPATION_NOT_FOUND예외() {
        given(participationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND));

        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("결제 준비 - 타인 참여 ID 요청 시 PAYMENT_ACCESS_DENIED 예외")
    void prepare_타인participation_PAYMENT_ACCESS_DENIED예외() {
        Participation participation = mockParticipationForPrepare(1L);
        given(participationRepository.findById(1L)).willReturn(Optional.of(participation));

        assertThatThrownBy(() -> paymentService.prepare(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));

        verify(paymentWriter, never()).save(any());
    }

    // ===== confirmDeposit =====

    @Test
    @DisplayName("보증금 결제 승인 - 정상 결제 성공 후 PAID 상태 반환")
    void confirmDeposit_정상요청_결제성공() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("50000"), 1L
        );
        Participation participation =
                mockParticipationForConfirm(BigDecimal.valueOf(50000));

        given(participation.getParticipationId()).willReturn(1L);
        Payment pendingPayment = pendingPaymentOf(participation);
        TossConfirmResponse tossResponse = new TossConfirmResponse(
                "test_paymentKey", "uuid-order-id", "DONE", new BigDecimal("50000")
        );

        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.of(pendingPayment));
        given(paymentRepository.findByTossPaymentKey("test_paymentKey")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "uuid-order-id", new BigDecimal("50000")))
                .willReturn(tossResponse);

        PaymentResponse response = paymentService.confirmDeposit(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paymentType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(response.tossPaymentKey()).isEqualTo("test_paymentKey");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50000"));
        verify(paymentWriter).save(pendingPayment);
    }

    @Test
    @DisplayName("보증금 결제 승인 - PENDING 결제 없을 시 ORDER_NOT_FOUND 예외")
    void confirmDeposit_PENDING없음_ORDER_NOT_FOUND예외() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("50000"), 1L
        );
        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("보증금 결제 승인 - 중복 paymentKey 요청 시 DUPLICATE_PAYMENT 예외")
    void confirmDeposit_중복paymentKey_DUPLICATE_PAYMENT예외() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("50000"), 1L
        );
        Payment pendingPayment = pendingPaymentOf(mock(Participation.class));

        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.of(pendingPayment));
        given(paymentRepository.findByTossPaymentKey("test_paymentKey"))
                .willReturn(Optional.of(Payment.builder().paymentId(2L)
                        .tossPaymentKey("test_paymentKey").status(PaymentStatus.PAID)
                        .createdAt(LocalDateTime.now()).build()));

        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_PAYMENT));

        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("보증금 결제 승인 - 금액 불일치 시 PAYMENT_AMOUNT_MISMATCH 예외")
    void confirmDeposit_금액불일치_PAYMENT_AMOUNT_MISMATCH예외() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("99999"), 1L
        );
        Participation participation = mockParticipationForConfirm(new BigDecimal("50000"));
        Payment pendingPayment = pendingPaymentOf(participation);

        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.of(pendingPayment));
        given(paymentRepository.findByTossPaymentKey("test_paymentKey")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));

        verify(tossPaymentClient, never()).confirm(anyString(), anyString(), any());
        verify(paymentWriter, never()).save(any());
    }

    @Test
    @DisplayName("보증금 결제 승인 - 토스 API 실패 시 TOSS_PAYMENT_FAILED 예외")
    void confirmDeposit_토스API실패_TOSS_PAYMENT_FAILED예외() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("50000"), 1L
        );
        Participation participation = mockParticipationForConfirm(new BigDecimal("50000"));
        Payment pendingPayment = pendingPaymentOf(participation);

        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.of(pendingPayment));
        given(paymentRepository.findByTossPaymentKey("test_paymentKey")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "uuid-order-id", new BigDecimal("50000")))
                .willThrow(new BusinessException(ErrorCode.TOSS_PAYMENT_FAILED));

        assertThatThrownBy(() -> paymentService.confirmDeposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOSS_PAYMENT_FAILED));

        verify(paymentWriter, never()).save(any());
    }

    // ===== confirmBalance =====

    @Test
    @DisplayName("잔액 결제 승인 - 정상 결제 성공 후 BALANCE 타입으로 PAID 반환")
    void confirmBalance_정상요청_결제성공() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("50000"), 1L
        );
        Participation participation =
                mockParticipationForConfirm(BigDecimal.valueOf(50000));
        given(participation.getParticipationId()).willReturn(1L);
        Payment pendingPayment = pendingPaymentOf(participation);
        TossConfirmResponse tossResponse = new TossConfirmResponse(
                "test_paymentKey", "uuid-order-id", "DONE", new BigDecimal("50000")
        );

        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.of(pendingPayment));
        given(paymentRepository.findByTossPaymentKey("test_paymentKey")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "uuid-order-id", new BigDecimal("50000")))
                .willReturn(tossResponse);

        PaymentResponse response = paymentService.confirmBalance(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paymentType()).isEqualTo(PaymentType.BALANCE);
        verify(paymentWriter).save(pendingPayment);
    }

    @Test
    @DisplayName("잔액 결제 승인 - 토스 API 실패 시 TOSS_PAYMENT_FAILED 예외")
    void confirmBalance_토스API실패_TOSS_PAYMENT_FAILED예외() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "test_paymentKey", new BigDecimal("50000"), 1L
        );
        Participation participation = mockParticipationForConfirm(new BigDecimal("50000"));
        Payment pendingPayment = pendingPaymentOf(participation);

        given(paymentRepository.findByParticipation_ParticipationIdAndStatus(1L, PaymentStatus.PENDING))
                .willReturn(Optional.of(pendingPayment));
        given(paymentRepository.findByTossPaymentKey("test_paymentKey")).willReturn(Optional.empty());
        given(tossPaymentClient.confirm("test_paymentKey", "uuid-order-id", new BigDecimal("50000")))
                .willThrow(new BusinessException(ErrorCode.TOSS_PAYMENT_FAILED));

        assertThatThrownBy(() -> paymentService.confirmBalance(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOSS_PAYMENT_FAILED));

        verify(paymentWriter, never()).save(any());
    }

    // ===== refund =====

    @Test
    @DisplayName("환불 - 정상 환불 성공")
    void refund_정상요청_환불성공() {
        PaymentRefundRequest request = new PaymentRefundRequest("변심");
        Participation participation = mockParticipationForRefund(1L);
        Payment payment = Payment.builder()
                .paymentId(1L).participation(participation).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();
        Payment refundedPayment = Payment.builder()
                .paymentId(1L).participation(participation).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.REFUNDED).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(paymentWriter.update(any(Payment.class), eq(PaymentStatus.REFUNDED))).willReturn(refundedPayment);

        PaymentResponse response = paymentService.refund(1L, request, 1L);

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        verify(tossPaymentClient).cancel("test_paymentKey", "변심");
        verify(paymentWriter).update(any(Payment.class), eq(PaymentStatus.REFUNDED));
    }

    @Test
    @DisplayName("환불 - 타인의 결제 환불 요청 시 PAYMENT_ACCESS_DENIED 예외")
    void refund_타인결제_PAYMENT_ACCESS_DENIED예외() {
        PaymentRefundRequest request = new PaymentRefundRequest("변심");
        Participation participation = mockParticipationForRefund(1L);
        Payment payment = Payment.builder()
                .paymentId(1L).participation(participation).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(1L, request, 2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - 존재하지 않는 결제 ID 요청 시 ORDER_NOT_FOUND 예외")
    void refund_존재하지않는결제_ORDER_NOT_FOUND예외() {
        PaymentRefundRequest request = new PaymentRefundRequest("변심");
        given(paymentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refund(999L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - 이미 환불된 결제 요청 시 ALREADY_REFUNDED 예외")
    void refund_이미환불된결제_ALREADY_REFUNDED예외() {
        PaymentRefundRequest request = new PaymentRefundRequest("변심");
        Participation participation = mockParticipationForRefund(1L);
        Payment refundedPayment = Payment.builder()
                .paymentId(1L).participation(participation).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.REFUNDED).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(refundedPayment));

        assertThatThrownBy(() -> paymentService.refund(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_REFUNDED));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - PAID 아닌 상태(FAILED 등) 요청 시 INVALID_PAYMENT_STATUS 예외")
    void refund_FAILED상태결제_INVALID_PAYMENT_STATUS예외() {
        PaymentRefundRequest request = new PaymentRefundRequest("변심");
        Participation participation = mockParticipationForRefund(1L);
        Payment failedPayment = Payment.builder()
                .paymentId(1L).participation(participation).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.FAILED).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(failedPayment));

        assertThatThrownBy(() -> paymentService.refund(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("환불 - 토스 API 실패 시 REFUND_FAILED 예외")
    void refund_토스API실패_REFUND_FAILED예외() {
        PaymentRefundRequest request = new PaymentRefundRequest("변심");
        Participation participation = mockParticipationForRefund(1L);
        Payment payment = Payment.builder()
                .paymentId(1L).participation(participation).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        willThrow(new BusinessException(ErrorCode.REFUND_FAILED))
                .given(tossPaymentClient).cancel(anyString(), anyString());

        assertThatThrownBy(() -> paymentService.refund(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.REFUND_FAILED));

        verify(paymentWriter, never()).update(any(), any());
    }

    // ===== refundByParticipationId =====

    @Test
    @DisplayName("참여 ID 환불 - 정상 환불 성공")
    void refundByParticipationId_정상환불성공() {
        Payment payment = Payment.builder()
                .paymentId(1L).participation(mock(Participation.class)).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.PAID).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findByParticipation_ParticipationId(1L)).willReturn(Optional.of(payment));

        paymentService.refundByParticipationId(1L);

        verify(tossPaymentClient).cancel("test_paymentKey", "참여 취소로 인한 환불");
        verify(paymentWriter).update(any(Payment.class), eq(PaymentStatus.REFUNDED));
    }

    @Test
    @DisplayName("참여 ID 환불 - 결제 내역 없을 시 ORDER_NOT_FOUND 예외")
    void refundByParticipationId_결제없음_ORDER_NOT_FOUND예외() {
        given(paymentRepository.findByParticipation_ParticipationId(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundByParticipationId(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }

    @Test
    @DisplayName("참여 ID 환불 - PAID 아닌 상태(이미 환불 등)는 Toss 호출 없이 스킵")
    void refundByParticipationId_PAID아닌상태_스킵() {
        Payment refundedPayment = Payment.builder()
                .paymentId(1L).participation(mock(Participation.class)).paymentType(PaymentType.DEPOSIT)
                .amount(new BigDecimal("50000")).tossPaymentKey("test_paymentKey")
                .orderId("test_orderId").status(PaymentStatus.REFUNDED).createdAt(LocalDateTime.now()).build();

        given(paymentRepository.findByParticipation_ParticipationId(1L)).willReturn(Optional.of(refundedPayment));

        paymentService.refundByParticipationId(1L);

        verify(tossPaymentClient, never()).cancel(anyString(), anyString());
        verify(paymentWriter, never()).update(any(), any());
    }
}
