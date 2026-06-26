package com.back.team9.moyeota.domain.funding.service;

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
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.doNothing;

@SpringBootTest
class FundingCancellationRefundIntegrationTest {

    @Autowired FundingService fundingService;
    @Autowired MemberRepository memberRepository;
    @Autowired FundingRepository fundingRepository;
    @Autowired PathinfoRepository pathinfoRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean MailService mailService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean ParticipationService participationService;
    @MockitoBean KakaoSocialLoginClient kakaoSocialLoginClient;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM payment");
        jdbcTemplate.execute("DELETE FROM participation");
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM pathinfo");
        jdbcTemplate.execute("DELETE FROM funding");
        jdbcTemplate.execute("DELETE FROM member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    // 펀딩 취소 → 전원 환불 통합 테스트
    // cancelFunding() 호출 → 3명 참여자 전원에게 ParticipationCancelledEvent 발행
    // → @Async 리스너 → refundByParticipationId() → 전원 REFUNDED
    @Test
    void cancelFunding_전원환불_비동기완료() {
        // given
        doNothing().when(tossPaymentClient).cancel(anyString(), anyString());

        String uid = UUID.randomUUID().toString().substring(0, 8);
        Member host = memberRepository.save(Member.builder()
                .email("host-" + uid + "@test.com")
                .name("방장")
                .nickname("host-" + uid)
                .phoneNumber("01000000001")
                .status(MemberStatus.ACTIVE)
                .build());

        Funding funding = fundingRepository.save(Funding.create(
                host, "취소테스트 펀딩", "내용",
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

        // 방장은 좌석 배정(Participation)은 있지만 결제하지 않음
        Seat hostSeat = seatRepository.save(Seat.builder()
                .pathinfo(pathinfo)
                .seatNumber("A0")
                .build());
        participationRepository.save(Participation.create(funding, host, hostSeat, null));

        List<Long> paymentIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Member participant = memberRepository.save(Member.builder()
                    .email("participant-" + i + "-" + uid + "@test.com")
                    .name("참여자" + i)
                    .nickname("nick-" + i + "-" + uid)
                    .phoneNumber("0100000000" + (i + 1))
                    .status(MemberStatus.ACTIVE)
                    .build());

            Seat seat = seatRepository.save(Seat.builder()
                    .pathinfo(pathinfo)
                    .seatNumber("A" + i)
                    .build());

            Participation participation = participationRepository.save(
                    Participation.create(funding, participant, seat, null)
            );

            Payment payment = paymentRepository.save(Payment.builder()
                    .participation(participation)
                    .orderId(UUID.randomUUID().toString())
                    .tossPaymentKey("test-key-" + UUID.randomUUID())
                    .amount(new BigDecimal("50000"))
                    .paymentType(PaymentType.DEPOSIT)
                    .status(PaymentStatus.PAID)
                    .build());

            paymentIds.add(payment.getPaymentId());
        }

        // when
        fundingService.cancelFunding(host.getMemberId(), funding.getFundingId());

        // then: 비동기 환불 완료까지 대기
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> paymentIds.stream()
                        .allMatch(id -> paymentRepository.findById(id)
                                .map(p -> p.getStatus() == PaymentStatus.REFUNDED)
                                .orElse(false)));

        paymentIds.forEach(id -> {
            Payment result = paymentRepository.findById(id).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        });
    }
}
