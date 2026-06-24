package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FinalAmountSchedulerTest {

    @Mock private FundingRepository fundingRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private Clock clock;

    @InjectMocks
    private FinalAmountScheduler scheduler;

    private final Instant fixedInstant = Instant.parse("2026-06-25T15:00:00Z"); // KST 자정
    private final ZoneId zone = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setupClock() {
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(zone);
    }

    private Member host() {
        return Member.builder()
                .memberId(1L)
                .email("host@test.com")
                .name("방장")
                .nickname("host")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .build();
    }

    private Funding confirmedFunding(Long fundingId, BigDecimal totalPrice) {
        return Funding.builder()
                .fundingId(fundingId)
                .member(host())
                .title("테스트 펀딩 " + fundingId)
                .departureDate(LocalDate.now(clock).plusDays(10))
                .status(FundingStatus.CONFIRMED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(false)
                .tripType(TripType.ONE_WAY)
                .totalPrice(totalPrice)
                .build();
    }

    private Participation activeParticipation(Long participationId) {
        return Participation.builder()
                .participationId(participationId)
                .funding(confirmedFunding(1L, new BigDecimal("500000")))
                .member(host())
                .paymentStatus(ParticipationPaymentStatus.PENDING)
                .finalAmount(BigDecimal.ZERO)
                .status(ParticipationStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - ACTIVE 참여자 2명일 때 totalPrice/2 (올림) 설정")
    void setFinalAmounts_참여자2명_정확한금액설정() {
        Funding funding = confirmedFunding(1L, new BigDecimal("500000"));
        Participation p1 = activeParticipation(1L);
        Participation p2 = activeParticipation(2L);

        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(
                1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(p1, p2));

        scheduler.setFinalAmounts();

        assertThat(p1.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(p2.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 나누어 떨어지지 않을 때 올림 처리")
    void setFinalAmounts_나누어떨어지지않음_올림처리() {
        Funding funding = confirmedFunding(1L, new BigDecimal("100000"));
        Participation p1 = activeParticipation(1L);
        Participation p2 = activeParticipation(2L);
        Participation p3 = activeParticipation(3L);

        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(
                1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(p1, p2, p3));

        scheduler.setFinalAmounts();

        // 100000 / 3 = 33333.33... → 올림 → 33334
        assertThat(p1.getFinalAmount()).isEqualByComparingTo(new BigDecimal("33334"));
        assertThat(p2.getFinalAmount()).isEqualByComparingTo(new BigDecimal("33334"));
        assertThat(p3.getFinalAmount()).isEqualByComparingTo(new BigDecimal("33334"));
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 이미 finalAmount 설정된 경우 멱등성 보장, 덮어쓰기 안 함")
    void setFinalAmounts_이미설정된경우_멱등성보장() {
        Funding funding = confirmedFunding(1L, new BigDecimal("500000"));
        Participation alreadySet = Participation.builder()
                .participationId(1L)
                .funding(funding)
                .member(host())
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(new BigDecimal("250000")) // 이미 설정됨
                .status(ParticipationStatus.ACTIVE)
                .build();

        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(
                1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(alreadySet));

        scheduler.setFinalAmounts();

        // 덮어쓰지 않았으므로 기존 값 유지
        assertThat(alreadySet.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 대상 펀딩 없을 시 아무 처리 없이 종료")
    void setFinalAmounts_대상없음_스킵() {
        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of());

        scheduler.setFinalAmounts();

        verify(participationRepository, never()).findByFunding_FundingIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - ACTIVE 참여자 없을 시 해당 펀딩 skip")
    void setFinalAmounts_참여자없음_스킵() {
        Funding funding = confirmedFunding(1L, new BigDecimal("500000"));

        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(
                1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of());

        scheduler.setFinalAmounts();

        // 참여자 없으므로 아무것도 호출되지 않음 — 예외 없이 정상 종료
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 개별 펀딩 처리 실패 시 나머지 건 계속 처리")
    void setFinalAmounts_개별실패_나머지건계속처리() {
        Funding funding1 = confirmedFunding(1L, new BigDecimal("500000"));
        Funding funding2 = confirmedFunding(2L, new BigDecimal("400000"));
        Participation p = activeParticipation(3L);

        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding1, funding2));
        given(participationRepository.findByFunding_FundingIdAndStatus(
                1L, ParticipationStatus.ACTIVE))
                .willThrow(new RuntimeException("DB 오류"));
        given(participationRepository.findByFunding_FundingIdAndStatus(
                2L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(p));

        scheduler.setFinalAmounts();

        // funding1 실패해도 funding2는 처리됨
        assertThat(p.getFinalAmount()).isEqualByComparingTo(new BigDecimal("400000"));
    }
}
