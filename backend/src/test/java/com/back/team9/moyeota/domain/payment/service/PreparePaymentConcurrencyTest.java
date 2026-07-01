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
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PreparePaymentConcurrencyTest {

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired FundingRepository fundingRepository;
    @Autowired PathinfoRepository pathinfoRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean MailService mailService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean ParticipationService participationService;
    @MockitoBean KakaoSocialLoginClient kakaoSocialLoginClient;

    @AfterEach
    void cleanup() {
        redisTemplate.delete(redisTemplate.keys("payment:prepare:*"));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM payment");
        jdbcTemplate.execute("DELETE FROM participation");
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM pathinfo");
        jdbcTemplate.execute("DELETE FROM funding");
        jdbcTemplate.execute("DELETE FROM member");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    @DisplayName("동일 participationId로 5회 동시 prepare() 호출 시 PENDING Payment는 1개여야 한다")
    void 동일_participationId_동시_prepare_호출시_PENDING은_1개() throws InterruptedException {
        // given
        Participation participation = givenParticipation();
        Long participationId = participation.getParticipationId();
        Long memberId = participation.getMember().getMemberId();

        int threadCount = 5;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when - 5개 스레드 동시 출발
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    paymentService.prepare(participationId, memberId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        List<Payment> pendings = paymentRepository
                .findAllByParticipation_ParticipationIdAndStatus(participationId, PaymentStatus.PENDING);

        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("성공: " + successCount.get() + "개");
        System.out.println("실패: " + failCount.get() + "개");
        System.out.println("PENDING Payment 수: " + pendings.size());
        pendings.forEach(p -> System.out.println("  orderId=" + p.getOrderId() + " amount=" + p.getAmount()));

        assertThat(pendings)
                .as("동시 prepare() 요청이 %d개여도 PENDING Payment는 1개여야 한다", threadCount)
                .hasSize(1);
    }

    private Participation givenParticipation() {
        Member member = memberRepository.save(Member.builder()
                .email("concurrency-" + UUID.randomUUID() + "@test.com")
                .name("테스터")
                .nickname("nick-" + UUID.randomUUID())
                .phoneNumber("01011112222")
                .status(MemberStatus.ACTIVE)
                .build());

        Funding funding = fundingRepository.save(Funding.create(
                member, "동시성 테스트 펀딩", "내용",
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

        return participationRepository.save(
                Participation.create(funding, member, seat, null)
        );
    }
}
