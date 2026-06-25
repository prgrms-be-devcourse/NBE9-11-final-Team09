package com.back.team9.moyeota.domain.participation.service;

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
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinalAmountServiceTest {

    @Mock private FundingRepository fundingRepository;
    @Mock private ParticipationRepository participationRepository;

    @InjectMocks
    private FinalAmountService finalAmountService;

    private Member host;
    private Funding funding;

    @BeforeEach
    void setUp() {
        host = Member.builder()
                .memberId(1L)
                .email("host@test.com")
                .name("방장")
                .nickname("host")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .build();

        funding = Funding.builder()
                .fundingId(1L)
                .member(host)
                .title("서울 → 부산 버스 대절")
                .departureDate(LocalDate.now().plusDays(10))
                .status(FundingStatus.CONFIRMED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(43)
                .paybackHold(false)
                .tripType(TripType.ONE_WAY)
                .totalPrice(new BigDecimal("500000"))
                .build();
    }

    private Participation activeParticipation(Long id) {
        return Participation.builder()
                .participationId(id)
                .funding(funding)
                .member(host)
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(BigDecimal.ZERO)
                .status(ParticipationStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("processFunding - 참여자 2명일 때 totalPrice/2 (올림) 설정")
    void processFunding_참여자2명_정확한금액설정() {
        Participation p1 = activeParticipation(1L);
        Participation p2 = activeParticipation(2L);

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(p1, p2));

        finalAmountService.processFunding(1L);

        assertThat(p1.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(p2.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("processFunding - 나누어 떨어지지 않을 때 올림 처리")
    void processFunding_나누어떨어지지않음_올림처리() {
        Funding fundingWith100k = Funding.builder()
                .fundingId(2L).member(host).title("테스트").departureDate(LocalDate.now().plusDays(10))
                .status(FundingStatus.CONFIRMED).busType(BusType.BUS_45).minParticipants(10)
                .maxParticipants(43).paybackHold(false).tripType(TripType.ONE_WAY)
                .totalPrice(new BigDecimal("100000")).build();

        Participation p1 = Participation.builder().participationId(1L).funding(fundingWith100k)
                .member(host).paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(BigDecimal.ZERO).status(ParticipationStatus.ACTIVE).build();
        Participation p2 = Participation.builder().participationId(2L).funding(fundingWith100k)
                .member(host).paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(BigDecimal.ZERO).status(ParticipationStatus.ACTIVE).build();
        Participation p3 = Participation.builder().participationId(3L).funding(fundingWith100k)
                .member(host).paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(BigDecimal.ZERO).status(ParticipationStatus.ACTIVE).build();

        given(fundingRepository.findById(2L)).willReturn(Optional.of(fundingWith100k));
        given(participationRepository.findByFunding_FundingIdAndStatus(2L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(p1, p2, p3));

        finalAmountService.processFunding(2L);

        // 100000 / 3 = 33333.33... → 100원 올림 → 33400
        assertThat(p1.getFinalAmount()).isEqualByComparingTo(new BigDecimal("33400"));
        assertThat(p2.getFinalAmount()).isEqualByComparingTo(new BigDecimal("33400"));
        assertThat(p3.getFinalAmount()).isEqualByComparingTo(new BigDecimal("33400"));
    }

    @Test
    @DisplayName("processFunding - 전원 이미 설정된 경우(allMatch) 멱등성 보장, 덮어쓰기 안 함")
    void processFunding_전원이미설정_멱등성보장() {
        Participation alreadySet = Participation.builder()
                .participationId(1L).funding(funding).member(host)
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(new BigDecimal("250000"))
                .status(ParticipationStatus.ACTIVE).build();

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(alreadySet));

        finalAmountService.processFunding(1L);

        assertThat(alreadySet.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("processFunding - 일부만 설정된 경우(allMatch=false) 전체 재설정하여 부분 실패 복구")
    void processFunding_일부만설정_전체재설정() {
        Participation set = Participation.builder()
                .participationId(1L).funding(funding).member(host)
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(new BigDecimal("250000"))
                .status(ParticipationStatus.ACTIVE).build();
        Participation notSet = Participation.builder()
                .participationId(2L).funding(funding).member(host)
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(BigDecimal.ZERO)
                .status(ParticipationStatus.ACTIVE).build();

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of(set, notSet));

        finalAmountService.processFunding(1L);

        // allMatch=false이므로 전체 재설정 (500000/2=250000)
        assertThat(set.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(notSet.getFinalAmount()).isEqualByComparingTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("processFunding - ACTIVE 참여자 없을 시 아무 처리 없이 종료")
    void processFunding_참여자없음_스킵() {
        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(participationRepository.findByFunding_FundingIdAndStatus(1L, ParticipationStatus.ACTIVE))
                .willReturn(List.of());

        finalAmountService.processFunding(1L);
        // 예외 없이 정상 종료
    }

    @Test
    @DisplayName("processFunding - 존재하지 않는 fundingId 요청 시 FUNDING_NOT_FOUND 예외")
    void processFunding_존재하지않는펀딩_FUNDING_NOT_FOUND예외() {
        given(fundingRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> finalAmountService.processFunding(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_NOT_FOUND));
    }
}
