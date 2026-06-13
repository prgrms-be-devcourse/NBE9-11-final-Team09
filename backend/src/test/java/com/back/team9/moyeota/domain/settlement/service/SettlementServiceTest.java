package com.back.team9.moyeota.domain.settlement.service;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.settlement.dto.SettlementCreateRequest;
import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.domain.settlement.repository.SettlementRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private FundingRepository fundingRepository;

    @InjectMocks
    private SettlementService settlementService;

    private Member hostMember;
    private Funding funding;

    @BeforeEach
    void setUp() {
        hostMember = Member.builder()
                .memberId(1L)
                .email("host@test.com")
                .name("방장")
                .nickname("host")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        funding = Funding.builder()
                .fundingId(1L)
                .member(hostMember)
                .title("서울 → 부산 버스 대절")
                .departureDate(LocalDateTime.now().plusDays(7))
                .status(FundingStatus.COMPLETED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("정산 생성 - 정상 요청 시 CALCULATED 상태로 정산 내역 생성, 수수료 계산 정확")
    void create_정상요청_정산내역생성성공() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(1L, 100000);

        int expectedPlatformFee = (int) (100000 * 0.05);   // 5000
        int expectedHostPaybackAmount = 100000 - expectedPlatformFee; // 95000

        Settlement savedSettlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(100000)
                .platformFee(expectedPlatformFee)
                .hostPaybackAmount(expectedHostPaybackAmount)
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)
                .createdAt(LocalDateTime.now())
                .build();

        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(false);
        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(settlementRepository.save(any(Settlement.class))).willReturn(savedSettlement);

        // When
        SettlementResponse response = settlementService.create(request);

        // Then - 응답값 검증
        assertThat(response.settlementId()).isEqualTo(1L);
        assertThat(response.totalAmount()).isEqualTo(100000);
        assertThat(response.platformFee()).isEqualTo(5000);
        assertThat(response.hostPaybackAmount()).isEqualTo(95000);
        assertThat(response.status()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(response.paybackHold()).isFalse();
        assertThat(response.paybackPaidAt()).isNull();

        // Then - 실제 save에 전달된 Settlement 값 검증
        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        Settlement captured = captor.getValue();
        assertThat(captured.getPlatformFee()).isEqualTo(5000);
        assertThat(captured.getHostPaybackAmount()).isEqualTo(95000);
        assertThat(captured.getStatus()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(captured.getMember()).isEqualTo(hostMember);
    }

    @Test
    @DisplayName("정산 생성 - Funding의 paybackHold=true면 Settlement도 paybackHold=true로 생성")
    void create_paybackHoldTrue인펀딩_정산도paybackHoldTrue() {
        // Given
        Funding holdFunding = Funding.builder()
                .fundingId(2L)
                .member(hostMember)
                .title("홀드 펀딩")
                .departureDate(LocalDateTime.now().plusDays(7))
                .status(FundingStatus.COMPLETED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(true)
                .createdAt(LocalDateTime.now())
                .build();

        SettlementCreateRequest request = new SettlementCreateRequest(2L, 100000);

        Settlement savedSettlement = Settlement.builder()
                .settlementId(2L)
                .member(hostMember)
                .funding(holdFunding)
                .totalAmount(100000)
                .platformFee(5000)
                .hostPaybackAmount(95000)
                .status(SettlementStatus.CALCULATED)
                .paybackHold(true)
                .createdAt(LocalDateTime.now())
                .build();

        given(settlementRepository.existsByFunding_FundingId(2L)).willReturn(false);
        given(fundingRepository.findById(2L)).willReturn(Optional.of(holdFunding));
        given(settlementRepository.save(any(Settlement.class))).willReturn(savedSettlement);

        // When
        SettlementResponse response = settlementService.create(request);

        // Then
        assertThat(response.paybackHold()).isTrue();

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        assertThat(captor.getValue().getPaybackHold()).isTrue();
    }

    @Test
    @DisplayName("정산 생성 - 이미 정산 내역 존재 시 SETTLEMENT_ALREADY_EXISTS 예외, 이후 로직 미실행")
    void create_이미정산내역존재_SETTLEMENT_ALREADY_EXISTS예외() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(1L, 100000);

        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_EXISTS));

        verify(fundingRepository, never()).findById(any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("정산 생성 - COMPLETED 아닌 펀딩 요청 시 SETTLEMENT_NOT_AVAILABLE 예외, save 미실행")
    void create_COMPLETED아닌펀딩_SETTLEMENT_NOT_AVAILABLE예외() {
        // Given
        Funding recruitingFunding = Funding.builder()
                .fundingId(1L)
                .member(hostMember)
                .title("모집 중 펀딩")
                .departureDate(LocalDateTime.now().plusDays(7))
                .status(FundingStatus.RECRUITING)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(false)
                .createdAt(LocalDateTime.now())
                .build();

        SettlementCreateRequest request = new SettlementCreateRequest(1L, 100000);

        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(false);
        given(fundingRepository.findById(1L)).willReturn(Optional.of(recruitingFunding));

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_AVAILABLE));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("정산 생성 - 존재하지 않는 펀딩 ID 요청 시 FUNDING_NOT_FOUND 예외, save 미실행")
    void create_존재하지않는펀딩_FUNDING_NOT_FOUND예외() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(999L, 100000);

        given(settlementRepository.existsByFunding_FundingId(999L)).willReturn(false);
        given(fundingRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_NOT_FOUND));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("정산 조회 - 존재하는 fundingId로 조회 성공")
    void getByFundingId_존재하는정산_조회성공() {
        // Given
        Settlement settlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(100000)
                .platformFee(5000)
                .hostPaybackAmount(95000)
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)
                .createdAt(LocalDateTime.now())
                .build();

        given(settlementRepository.findByFunding_FundingId(1L)).willReturn(Optional.of(settlement));

        // When
        SettlementResponse response = settlementService.getByFundingId(1L);

        // Then
        assertThat(response.settlementId()).isEqualTo(1L);
        assertThat(response.totalAmount()).isEqualTo(100000);
        assertThat(response.platformFee()).isEqualTo(5000);
        assertThat(response.hostPaybackAmount()).isEqualTo(95000);
        assertThat(response.status()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(response.paybackPaidAt()).isNull();
    }

    @Test
    @DisplayName("정산 조회 - 존재하지 않는 fundingId 요청 시 SETTLEMENT_NOT_FOUND 예외 발생")
    void getByFundingId_존재하지않는정산_SETTLEMENT_NOT_FOUND예외() {
        // Given
        given(settlementRepository.findByFunding_FundingId(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.getByFundingId(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND));
    }
}
