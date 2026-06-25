package com.back.team9.moyeota.domain.settlement.service;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private SettlementService settlementService;

    private Member hostMember;
    private Funding funding;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.now());
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        ReflectionTestUtils.setField(settlementService, "platformFeeRate", new BigDecimal("0.10"));

        hostMember = Member.builder()
                .memberId(1L)
                .email("host@test.com")
                .name("방장")
                .nickname("host")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)

                .build();

        funding = Funding.builder()
                .fundingId(1L)
                .member(hostMember)
                .title("서울 → 부산 버스 대절")
                .departureDate(LocalDate.now().plusDays(7))
                .status(FundingStatus.COMPLETED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(false)

                .build();
    }

    @Test
    @DisplayName("정산 생성 - 정상 요청 시 CALCULATED 상태로 정산 내역 생성, 수수료 계산 정확")
    void create_정상요청_정산내역생성성공() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(1L);

        Settlement savedSettlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)

                .build();

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(false);
        given(paymentRepository.sumAmountByFundingIdAndStatus(1L, PaymentStatus.PAID)).willReturn(new BigDecimal("100000"));
        given(settlementRepository.saveAndFlush(any(Settlement.class))).willReturn(savedSettlement);

        // When
        SettlementResponse response = settlementService.create(request, 1L);

        // Then - 응답값 검증
        assertThat(response.settlementId()).isEqualTo(1L);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.platformFee()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.hostPaybackAmount()).isEqualByComparingTo(new BigDecimal("90000"));
        assertThat(response.status()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(response.paybackHold()).isFalse();
        assertThat(response.paybackPaidAt()).isNull();

        // Then - 실제 save에 전달된 Settlement 값 검증
        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).saveAndFlush(captor.capture());
        Settlement captured = captor.getValue();
        assertThat(captured.getPlatformFee()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(captured.getHostPaybackAmount()).isEqualByComparingTo(new BigDecimal("90000"));
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
                .departureDate(LocalDate.now().plusDays(7))
                .status(FundingStatus.COMPLETED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(true)

                .build();

        SettlementCreateRequest request = new SettlementCreateRequest(2L);

        Settlement savedSettlement = Settlement.builder()
                .settlementId(2L)
                .member(hostMember)
                .funding(holdFunding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(true)

                .build();

        given(fundingRepository.findById(2L)).willReturn(Optional.of(holdFunding));
        given(settlementRepository.existsByFunding_FundingId(2L)).willReturn(false);
        given(paymentRepository.sumAmountByFundingIdAndStatus(2L, PaymentStatus.PAID)).willReturn(new BigDecimal("100000"));
        given(settlementRepository.saveAndFlush(any(Settlement.class))).willReturn(savedSettlement);

        // When
        SettlementResponse response = settlementService.create(request, 1L);

        // Then
        assertThat(response.paybackHold()).isTrue();

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPaybackHold()).isTrue();
    }

    @Test
    @DisplayName("정산 생성 - 방장이 아닌 멤버 요청 시 SETTLEMENT_ACCESS_DENIED 예외, save 미실행")
    void create_방장이아닌멤버요청_SETTLEMENT_ACCESS_DENIED예외() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(1L);
        Long otherMemberId = 999L;

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request, otherMemberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_ACCESS_DENIED));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("정산 생성 - 이미 정산 내역 존재 시 SETTLEMENT_ALREADY_EXISTS 예외, save 미실행")
    void create_이미정산내역존재_SETTLEMENT_ALREADY_EXISTS예외() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(1L);

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_EXISTS));

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
                .departureDate(LocalDate.now().plusDays(7))
                .status(FundingStatus.RECRUITING)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(false)

                .build();

        SettlementCreateRequest request = new SettlementCreateRequest(1L);

        given(fundingRepository.findById(1L)).willReturn(Optional.of(recruitingFunding));

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_AVAILABLE));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("정산 생성 - 존재하지 않는 펀딩 ID 요청 시 FUNDING_NOT_FOUND 예외, save 미실행")
    void create_존재하지않는펀딩_FUNDING_NOT_FOUND예외() {
        // Given
        SettlementCreateRequest request = new SettlementCreateRequest(999L);

        given(fundingRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.create(request, 1L))
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
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)

                .build();

        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
        given(settlementRepository.findByFunding_FundingId(1L)).willReturn(Optional.of(settlement));

        // When
        SettlementResponse response = settlementService.getByFundingId(1L, 1L);

        // Then
        assertThat(response.settlementId()).isEqualTo(1L);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.platformFee()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.hostPaybackAmount()).isEqualByComparingTo(new BigDecimal("90000"));
        assertThat(response.status()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(response.paybackPaidAt()).isNull();
    }

    @Test
    @DisplayName("정산 조회 - 존재하지 않는 fundingId 요청 시 SETTLEMENT_NOT_FOUND 예외 발생")
    void getByFundingId_존재하지않는정산_SETTLEMENT_NOT_FOUND예외() {
        // Given
        given(fundingRepository.findById(999L)).willReturn(Optional.of(funding));
        given(settlementRepository.findByFunding_FundingId(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.getByFundingId(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("정산 조회 - 방장이 아닌 멤버 요청 시 SETTLEMENT_ACCESS_DENIED 예외")
    void getByFundingId_방장이아닌멤버요청_SETTLEMENT_ACCESS_DENIED예외() {
        // Given
        Long otherMemberId = 999L;
        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));

        // When & Then
        assertThatThrownBy(() -> settlementService.getByFundingId(1L, otherMemberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("페이백 승인 - paybackHold=false인 정산 요청 시 SETTLEMENT_MANUAL_NOT_REQUIRED 예외")
    void approve_paybackHoldFalse인정산_SETTLEMENT_MANUAL_NOT_REQUIRED예외() {
        // Given
        Settlement autoTarget = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(autoTarget));

        // When & Then
        assertThatThrownBy(() -> settlementService.approve(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("페이백 승인 - 정상 요청 시 APPROVED 상태 응답, paybackPaidAt 설정, save() 미호출(dirty checking)")
    void approve_정상요청_APPROVED상태응답반환() {
        // Given
        Settlement settlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(true)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

        // When
        SettlementResponse response = settlementService.approve(1L);

        // Then
        assertThat(response.status()).isEqualTo(SettlementStatus.APPROVED);
        assertThat(response.paybackPaidAt()).isNotNull();
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("페이백 승인 - 존재하지 않는 settlementId 요청 시 SETTLEMENT_NOT_FOUND 예외")
    void approve_존재하지않는settlementId_SETTLEMENT_NOT_FOUND예외() {
        // Given
        given(settlementRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.approve(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("페이백 승인 - CALCULATED 아닌 상태(APPROVED)에서 재승인 요청 시 SETTLEMENT_NOT_AVAILABLE 예외")
    void approve_CALCULATED아닌상태_SETTLEMENT_NOT_AVAILABLE예외() {
        // Given
        Settlement alreadyApproved = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.APPROVED)
                .paybackHold(true)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(alreadyApproved));

        // When & Then
        assertThatThrownBy(() -> settlementService.approve(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_AVAILABLE));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("페이백 거절 - paybackHold=false인 정산 요청 시 SETTLEMENT_MANUAL_NOT_REQUIRED 예외")
    void reject_paybackHoldFalse인정산_SETTLEMENT_MANUAL_NOT_REQUIRED예외() {
        // Given
        Settlement autoTarget = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(autoTarget));

        // When & Then
        assertThatThrownBy(() -> settlementService.reject(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("페이백 거절 - 정상 요청 시 REJECTED 상태 응답, paybackPaidAt null 유지, save() 미호출(dirty checking)")
    void reject_정상요청_REJECTED상태응답반환() {
        // Given
        Settlement settlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(true)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

        // When
        SettlementResponse response = settlementService.reject(1L);

        // Then
        assertThat(response.status()).isEqualTo(SettlementStatus.REJECTED);
        assertThat(response.paybackPaidAt()).isNull();
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("페이백 거절 - 존재하지 않는 settlementId 요청 시 SETTLEMENT_NOT_FOUND 예외")
    void reject_존재하지않는settlementId_SETTLEMENT_NOT_FOUND예외() {
        // Given
        given(settlementRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.reject(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("페이백 거절 - CALCULATED 아닌 상태(REJECTED)에서 재거절 요청 시 SETTLEMENT_NOT_AVAILABLE 예외")
    void reject_CALCULATED아닌상태_SETTLEMENT_NOT_AVAILABLE예외() {
        // Given
        Settlement alreadyRejected = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.REJECTED)
                .paybackHold(true)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(alreadyRejected));

        // When & Then
        assertThatThrownBy(() -> settlementService.reject(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_AVAILABLE));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("페이백 승인 - REJECTED 상태에서 승인 시도 시 SETTLEMENT_NOT_AVAILABLE 예외 (역방향 전환 방지)")
    void approve_REJECTED상태에서승인시도_SETTLEMENT_NOT_AVAILABLE예외() {
        // Given
        Settlement rejectedSettlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.REJECTED)
                .paybackHold(true)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(rejectedSettlement));

        // When & Then
        assertThatThrownBy(() -> settlementService.approve(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("페이백 거절 - APPROVED 상태에서 거절 시도 시 SETTLEMENT_NOT_AVAILABLE 예외 (역방향 전환 방지)")
    void reject_APPROVED상태에서거절시도_SETTLEMENT_NOT_AVAILABLE예외() {
        // Given
        Settlement approvedSettlement = Settlement.builder()
                .settlementId(1L)
                .member(hostMember)
                .funding(funding)
                .totalAmount(new BigDecimal("100000"))
                .platformFee(new BigDecimal("10000"))
                .hostPaybackAmount(new BigDecimal("90000"))
                .status(SettlementStatus.APPROVED)
                .paybackHold(true)

                .build();

        given(settlementRepository.findById(1L)).willReturn(Optional.of(approvedSettlement));

        // When & Then
        assertThatThrownBy(() -> settlementService.reject(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SETTLEMENT_NOT_AVAILABLE));
    }

    // ===== createByScheduler =====

    @Test
    @DisplayName("스케줄러 정산 생성 - paybackHold=false면 COMPLETED 상태로 저장, paybackPaidAt 설정")
    void createByScheduler_paybackHoldFalse_COMPLETED상태저장() {
        // Given
        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(false);
        given(fundingRepository.findById(1L)).willReturn(Optional.of(funding)); // paybackHold=false
        given(paymentRepository.sumAmountByFundingIdAndStatus(1L, PaymentStatus.PAID))
                .willReturn(new BigDecimal("100000"));

        // When
        settlementService.createByScheduler(1L);

        // Then
        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        Settlement saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(saved.getPaybackPaidAt()).isNotNull();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(saved.getPlatformFee()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(saved.getHostPaybackAmount()).isEqualByComparingTo(new BigDecimal("90000"));
        assertThat(saved.getPaybackHold()).isFalse();
    }

    @Test
    @DisplayName("스케줄러 정산 생성 - paybackHold=true면 CALCULATED 상태로 저장, paybackPaidAt null")
    void createByScheduler_paybackHoldTrue_CALCULATED상태저장() {
        // Given
        Funding holdFunding = Funding.builder()
                .fundingId(2L)
                .member(hostMember)
                .title("신고 있는 펀딩")
                .departureDate(LocalDate.now().minusDays(1))
                .status(FundingStatus.COMPLETED)
                .busType(BusType.BUS_45)
                .minParticipants(10)
                .maxParticipants(45)
                .paybackHold(true)
                .build();

        given(settlementRepository.existsByFunding_FundingId(2L)).willReturn(false);
        given(fundingRepository.findById(2L)).willReturn(Optional.of(holdFunding));
        given(paymentRepository.sumAmountByFundingIdAndStatus(2L, PaymentStatus.PAID))
                .willReturn(new BigDecimal("100000"));

        // When
        settlementService.createByScheduler(2L);

        // Then
        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captor.capture());
        Settlement saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.CALCULATED);
        assertThat(saved.getPaybackPaidAt()).isNull();
        assertThat(saved.getPaybackHold()).isTrue();
    }

    @Test
    @DisplayName("스케줄러 정산 생성 - 이미 정산 존재 시 멱등성 보장, save 미실행")
    void createByScheduler_이미정산존재_멱등성보장() {
        // Given
        given(settlementRepository.existsByFunding_FundingId(1L)).willReturn(true);

        // When
        settlementService.createByScheduler(1L);

        // Then
        verify(fundingRepository, never()).findById(any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("스케줄러 정산 생성 - 존재하지 않는 fundingId 요청 시 FUNDING_NOT_FOUND 예외")
    void createByScheduler_존재하지않는펀딩_FUNDING_NOT_FOUND예외() {
        // Given
        given(settlementRepository.existsByFunding_FundingId(999L)).willReturn(false);
        given(fundingRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settlementService.createByScheduler(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_NOT_FOUND));

        verify(settlementRepository, never()).save(any());
    }
}
