package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.participation.service.FinalAmountService;
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
class FinalAmountSchedulerTest {

    @Mock private FundingRepository fundingRepository;
    @Mock private FinalAmountService finalAmountService;
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

    private Funding confirmedFunding(Long fundingId) {
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
                .departureDate(LocalDate.now(clock).plusDays(10))
                .status(FundingStatus.CONFIRMED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(43)
                .paybackHold(false)
                .tripType(TripType.ONE_WAY)
                .totalPrice(new BigDecimal("500000"))
                .build();
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - CONFIRMED 펀딩 존재 시 processFunding 호출")
    void setFinalAmounts_CONFIRMED펀딩존재_processFunding호출() {
        Funding funding = confirmedFunding(1L);
        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding));

        scheduler.setFinalAmounts();

        verify(finalAmountService).processFunding(1L);
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 여러 펀딩 존재 시 각각 processFunding 호출")
    void setFinalAmounts_복수건_각각처리() {
        Funding funding1 = confirmedFunding(1L);
        Funding funding2 = confirmedFunding(2L);
        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding1, funding2));

        scheduler.setFinalAmounts();

        verify(finalAmountService).processFunding(1L);
        verify(finalAmountService).processFunding(2L);
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 대상 없을 시 아무 처리 없이 종료")
    void setFinalAmounts_대상없음_스킵() {
        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of());

        scheduler.setFinalAmounts();

        verify(finalAmountService, never()).processFunding(any());
    }

    @Test
    @DisplayName("finalAmount 스케줄러 - 개별 펀딩 처리 실패 시 나머지 건 계속 처리")
    void setFinalAmounts_개별실패_나머지건계속처리() {
        Funding funding1 = confirmedFunding(1L);
        Funding funding2 = confirmedFunding(2L);
        given(fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, LocalDate.now(clock).plusDays(10)))
                .willReturn(List.of(funding1, funding2));
        willThrow(new RuntimeException("DB 오류"))
                .given(finalAmountService).processFunding(1L);

        scheduler.setFinalAmounts();

        verify(finalAmountService).processFunding(1L);
        verify(finalAmountService).processFunding(2L);
    }
}
