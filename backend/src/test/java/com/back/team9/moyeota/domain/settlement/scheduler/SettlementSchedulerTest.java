package com.back.team9.moyeota.domain.settlement.scheduler;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.settlement.service.SettlementService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock private FundingRepository fundingRepository;
    @Mock private SettlementService settlementService;
    @Mock private Clock clock;

    @InjectMocks
    private SettlementScheduler scheduler;

    private final Instant fixedInstant = Instant.parse("2026-06-25T01:00:00Z"); // KST 10:00
    private final ZoneId zone = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setupClock() {
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(zone);
    }

    private Funding completedFunding(Long fundingId) {
        Member host = Member.builder()
                .memberId(1L)
                .email("host@test.com")
                .name("방장")
                .nickname("host")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .build();

        return Funding.builder()
                .fundingId(fundingId)
                .member(host)
                .title("테스트 펀딩 " + fundingId)
                .departureDate(LocalDate.now(clock).minusDays(1))
                .status(FundingStatus.COMPLETED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(false)
                .tripType(TripType.ONE_WAY)
                .totalPrice(new BigDecimal("500000"))
                .build();
    }

    @Test
    @DisplayName("정산 스케줄러 - COMPLETED 펀딩 존재 시 createByScheduler 호출")
    void createSettlements_COMPLETED펀딩존재_createByScheduler호출() {
        Funding funding = completedFunding(1L);
        given(fundingRepository.findByStatusAndDepartureDateBefore(
                FundingStatus.COMPLETED, LocalDate.now(clock)))
                .willReturn(List.of(funding));

        scheduler.createSettlements();

        verify(settlementService).createByScheduler(1L);
    }

    @Test
    @DisplayName("정산 스케줄러 - 여러 COMPLETED 펀딩 존재 시 각각 createByScheduler 호출")
    void createSettlements_복수건_각각처리() {
        Funding funding1 = completedFunding(1L);
        Funding funding2 = completedFunding(2L);
        given(fundingRepository.findByStatusAndDepartureDateBefore(
                FundingStatus.COMPLETED, LocalDate.now(clock)))
                .willReturn(List.of(funding1, funding2));

        scheduler.createSettlements();

        verify(settlementService).createByScheduler(1L);
        verify(settlementService).createByScheduler(2L);
    }

    @Test
    @DisplayName("정산 스케줄러 - 대상 없을 시 아무 처리 없이 종료")
    void createSettlements_대상없음_스킵() {
        given(fundingRepository.findByStatusAndDepartureDateBefore(
                FundingStatus.COMPLETED, LocalDate.now(clock)))
                .willReturn(List.of());

        scheduler.createSettlements();

        verify(settlementService, never()).createByScheduler(any());
    }

    @Test
    @DisplayName("정산 스케줄러 - 개별 펀딩 처리 실패 시 로그 후 나머지 건 계속 처리")
    void createSettlements_개별실패_나머지건계속처리() {
        Funding funding1 = completedFunding(1L);
        Funding funding2 = completedFunding(2L);
        given(fundingRepository.findByStatusAndDepartureDateBefore(
                FundingStatus.COMPLETED, LocalDate.now(clock)))
                .willReturn(List.of(funding1, funding2));
        willThrow(new RuntimeException("DB 오류"))
                .given(settlementService).createByScheduler(1L);

        scheduler.createSettlements();

        verify(settlementService).createByScheduler(1L);
        verify(settlementService).createByScheduler(2L);
    }
}
