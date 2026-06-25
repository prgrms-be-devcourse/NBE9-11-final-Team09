package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.infrastructure.social.KakaoSocialLoginClient;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest
class PaymentIntegrationTest {

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired FundingRepository fundingRepository;
    @Autowired PathinfoRepository pathinfoRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean MailService mailService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean ParticipationService participationService;
    @MockitoBean KakaoSocialLoginClient kakaoSocialLoginClient;

    @AfterEach
    void cleanup() {
        // H2 FK 제약 일시 해제 후 전체 삭제
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM payment");
        jdbcTemplate.execute("DELETE FROM participation");
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM pathinfo");
        jdbcTemplate.execute("DELETE FROM funding");
        jdbcTemplate.execute("DELETE FROM member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    // ===== 케이스 4: 사용자 직접 환불 — 정상 경로 =====

    @Test
    void refund_정상_REFUNDED() {
        Payment payment = givenPaidDepositPayment();
        doNothing().when(tossPaymentClient).cancel(anyString(), anyString());

        paymentService.refund(
                payment.getPaymentId(),
                new PaymentRefundRequest("테스트 취소"),
                payment.getParticipation().getMember().getMemberId()
        );

        Payment result = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // ===== 케이스 4: 사용자 직접 환불 — Toss 실패 시 @Transactional 롤백 → PAID 복구 =====

    @Test
    void refund_Toss실패_DB는PAID유지() {
        Payment payment = givenPaidDepositPayment();
        willThrow(new RuntimeException("Toss 장애"))
                .given(tossPaymentClient).cancel(anyString(), anyString());

        assertThatThrownBy(() -> paymentService.refund(
                payment.getPaymentId(),
                new PaymentRefundRequest("테스트 취소"),
                payment.getParticipation().getMember().getMemberId()
        ));

        // refund()의 @Transactional이 롤백 → REFUND_PENDING 미커밋 → DB는 PAID
        Payment result = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    // ===== 케이스 6: 잔액 결제 환불 없음 — DEPOSIT만 환불, BALANCE 스킵 =====

    @Test
    void refundByParticipationId_DEPOSIT만환불_BALANCE스킵() {
        Participation participation = givenParticipationWithDepositAndBalance();
        doNothing().when(tossPaymentClient).cancel(anyString(), anyString());

        paymentService.refundByParticipationId(participation.getParticipationId());

        paymentRepository.findByParticipation_ParticipationId(participation.getParticipationId())
                .forEach(p -> {
                    if (p.getPaymentType() == PaymentType.DEPOSIT) {
                        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
                    } else {
                        // BALANCE는 환불하지 않음
                        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
                    }
                });
    }

    // ===== 케이스 5/7: 참여 취소 이벤트 → @Async 리스너 → 환불 완료 (Awaitility) =====

    @Test
    void participationCancelled_비동기리스너_환불완료() {
        Payment payment = givenPaidDepositPayment();
        Long participationId = payment.getParticipation().getParticipationId();
        doNothing().when(tossPaymentClient).cancel(anyString(), anyString());

        // AFTER_COMMIT 리스너 트리거: TX 내부에서 이벤트 발행 → TX 커밋 후 리스너 실행
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new ParticipationCancelledEvent(participationId));
            return null;
        });

        // refundExecutor 스레드풀에서 비동기 환불 완료까지 대기
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> paymentRepository.findById(payment.getPaymentId())
                        .map(p -> p.getStatus() == PaymentStatus.REFUNDED)
                        .orElse(false));
    }

    // ===== Fixtures =====

    private Payment givenPaidDepositPayment() {
        Member member = memberRepository.save(Member.builder()
                .email("pay-test-" + UUID.randomUUID() + "@test.com")
                .name("테스터")
                .nickname("nick-" + UUID.randomUUID())
                .phoneNumber("01011112222")
                .status(MemberStatus.ACTIVE)
                .build());

        Funding funding = fundingRepository.save(Funding.create(
                member, "테스트 펀딩", "내용",
                LocalDate.now().plusDays(30),
                BusType.BUS_25, 5,
                new BigDecimal("1000000"), TripType.ONE_WAY
        ));

        Pathinfo pathinfo = pathinfoRepository.save(Pathinfo.create(
                funding,
                LocalDateTime.now().plusDays(30),
                "서울역", Region.SEOUL,
                "부산역", Region.BUSAN,
                Direction.OUTBOUND
        ));

        Seat seat = seatRepository.save(Seat.builder()
                .pathinfo(pathinfo)
                .seatNumber("A1")
                .build());

        Participation participation = participationRepository.save(
                Participation.create(funding, member, seat, null)
        );

        return paymentRepository.save(Payment.builder()
                .participation(participation)
                .orderId(UUID.randomUUID().toString())
                .tossPaymentKey("test-key-" + UUID.randomUUID())
                .amount(new BigDecimal("50000"))
                .paymentType(PaymentType.DEPOSIT)
                .status(PaymentStatus.PAID)
                .build());
    }

    private Participation givenParticipationWithDepositAndBalance() {
        Member member = memberRepository.save(Member.builder()
                .email("bal-test-" + UUID.randomUUID() + "@test.com")
                .name("테스터2")
                .nickname("bal-nick-" + UUID.randomUUID())
                .phoneNumber("01022223333")
                .status(MemberStatus.ACTIVE)
                .build());

        Funding funding = fundingRepository.save(Funding.create(
                member, "테스트 펀딩2", "내용",
                LocalDate.now().plusDays(30),
                BusType.BUS_25, 5,
                new BigDecimal("1000000"), TripType.ONE_WAY
        ));

        Pathinfo pathinfo = pathinfoRepository.save(Pathinfo.create(
                funding,
                LocalDateTime.now().plusDays(30),
                "서울역", Region.SEOUL,
                "부산역", Region.BUSAN,
                Direction.OUTBOUND
        ));

        Seat seat = seatRepository.save(Seat.builder()
                .pathinfo(pathinfo)
                .seatNumber("A1")
                .build());

        Participation participation = participationRepository.save(
                Participation.create(funding, member, seat, null)
        );

        paymentRepository.save(Payment.builder()
                .participation(participation)
                .orderId(UUID.randomUUID().toString())
                .tossPaymentKey("dep-key-" + UUID.randomUUID())
                .amount(new BigDecimal("50000"))
                .paymentType(PaymentType.DEPOSIT)
                .status(PaymentStatus.PAID)
                .build());

        paymentRepository.save(Payment.builder()
                .participation(participation)
                .orderId(UUID.randomUUID().toString())
                .tossPaymentKey("bal-key-" + UUID.randomUUID())
                .amount(new BigDecimal("450000"))
                .paymentType(PaymentType.BALANCE)
                .status(PaymentStatus.PAID)
                .build());

        return participation;
    }
}
