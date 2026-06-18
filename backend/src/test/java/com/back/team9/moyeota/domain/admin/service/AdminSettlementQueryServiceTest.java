package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.AdminSettlementDetailResponse;
import com.back.team9.moyeota.domain.admin.dto.AdminSettlementListResponse;
import com.back.team9.moyeota.domain.admin.repository.AdminPaymentQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.AdminSettlementQueryRepository;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 정산 조회 서비스 테스트")
class AdminSettlementQueryServiceTest {

    @Mock
    private AdminSettlementQueryRepository settlementRepository;

    @Mock
    private AdminPaymentQueryRepository paymentRepository;

    @InjectMocks
    private AdminSettlementQueryService adminSettlementQueryService;

    @Test
    @DisplayName("정산 목록을 페이징 조회한다")
    void getSettlementsReturnsPagedSettlements() {
        // Given
        PageRequest pageable = PageRequest.of(0, 20);
        Settlement settlement = createSettlement();

        when(settlementRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(
                        List.of(settlement),
                        pageable,
                        1
                ));

        // When
        PageResponse<AdminSettlementListResponse> response =
                adminSettlementQueryService.getSettlements(pageable);

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().settlementId())
                .isEqualTo(1L);
        assertThat(response.content().getFirst().fundingId()).isEqualTo(10L);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("정산 상세 정보를 조회한다")
    void getSettlementReturnsSettlementDetail() {
        // Given
        Settlement settlement = createSettlement();

        when(settlementRepository.findBySettlementId(1L))
                .thenReturn(Optional.of(settlement));
        when(paymentRepository.countByParticipation_Funding_FundingIdAndStatus(
                10L,
                PaymentStatus.PAID
        )).thenReturn(30L);
        when(paymentRepository.countByParticipation_Funding_FundingIdAndPaymentTypeAndStatus(
                10L,
                PaymentType.DEPOSIT,
                PaymentStatus.PAID
        )).thenReturn(30L);
        when(paymentRepository.countByParticipation_Funding_FundingIdAndPaymentTypeAndStatus(
                10L,
                PaymentType.BALANCE,
                PaymentStatus.PAID
        )).thenReturn(25L);
        when(paymentRepository.sumAmountByFundingIdAndStatus(
                10L,
                PaymentStatus.PAID
        )).thenReturn(600000L);

        // When
        AdminSettlementDetailResponse response =
                adminSettlementQueryService.getSettlement(1L);

        // Then
        assertThat(response.settlementId()).isEqualTo(1L);
        assertThat(response.fundingId()).isEqualTo(10L);
        assertThat(response.paymentSummary().totalPaidCount())
                .isEqualTo(30L);
        assertThat(response.paymentSummary().depositPaidCount())
                .isEqualTo(30L);
        assertThat(response.paymentSummary().balancePaidCount())
                .isEqualTo(25L);
        assertThat(response.paymentSummary().totalPaidAmount())
                .isEqualTo(600000L);
    }

    @Test
    @DisplayName("존재하지 않는 정산 상세 조회 시 예외가 발생한다")
    void getUnknownSettlementThrowsException() {
        // Given
        when(settlementRepository.findBySettlementId(1L))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> adminSettlementQueryService.getSettlement(1L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND)
                );
    }

    private Settlement createSettlement() {
        return Settlement.builder()
                .settlementId(1L)
                .member(createMember())
                .funding(createFunding())
                .totalAmount(BigDecimal.valueOf(600000))
                .platformFee(BigDecimal.ZERO)
                .hostPaybackAmount(BigDecimal.valueOf(600000))
                .status(SettlementStatus.CALCULATED)
                .paybackHold(false)
                .createdAt(LocalDateTime.of(2026, 6, 20, 23, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 20, 23, 0))
                .build();
    }

    private Funding createFunding() {
        return Funding.builder()
                .fundingId(10L)
                .member(createMember())
                .title("잠실 경기 후 인천행 버스")
                .content("함께 이동할 참여자를 모집합니다.")
                .departureDate(LocalDate.of(2026, 7, 10))
                .busType(BusType.BUS_45)
                .status(FundingStatus.COMPLETED)
                .minParticipants(20)
                .maxParticipants(44)
                .totalPrice(600000)
                .paybackHold(false)
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
    }

    private Member createMember() {
        return Member.builder()
                .memberId(1L)
                .email("host@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("버스방장")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
    }
}
