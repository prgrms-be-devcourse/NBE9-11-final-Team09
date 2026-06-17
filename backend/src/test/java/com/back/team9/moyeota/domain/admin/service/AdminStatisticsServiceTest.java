package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.AdminStatisticsResponse;
import com.back.team9.moyeota.domain.admin.repository.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.AdminMemberQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.AdminPaymentQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.AdminSettlementQueryRepository;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 통계 서비스 테스트")
class AdminStatisticsServiceTest {

    @Mock
    private AdminMemberQueryRepository memberRepository;

    @Mock
    private AdminFundingQueryRepository fundingRepository;

    @Mock
    private AdminSettlementQueryRepository settlementRepository;

    @Mock
    private AdminPaymentQueryRepository paymentRepository;

    @InjectMocks
    private AdminStatisticsService adminStatisticsService;

    @Test
    @DisplayName("서비스 통계를 조회한다")
    void getStatisticsReturnsServiceStatistics() {
        // Given
        when(memberRepository.count()).thenReturn(100L);
        when(memberRepository.countByStatus(MemberStatus.ACTIVE))
                .thenReturn(90L);
        when(memberRepository.countByStatus(MemberStatus.WITHDRAWN))
                .thenReturn(5L);
        when(fundingRepository.countByStatus(FundingStatus.RECRUITING))
                .thenReturn(10L);
        when(fundingRepository.countByStatus(FundingStatus.COMPLETED))
                .thenReturn(20L);
        when(fundingRepository.countByStatus(FundingStatus.CANCELLED))
                .thenReturn(3L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.PAID))
                .thenReturn(4820000L);
        when(settlementRepository.countByStatus(SettlementStatus.CALCULATED))
                .thenReturn(7L);

        // When
        AdminStatisticsResponse response =
                adminStatisticsService.getStatistics();

        // Then
        assertThat(response.totalUsers()).isEqualTo(100L);
        assertThat(response.activeUsers()).isEqualTo(90L);
        assertThat(response.withdrawnUsers()).isEqualTo(5L);
        assertThat(response.activeFundings()).isEqualTo(10L);
        assertThat(response.completedFundings()).isEqualTo(20L);
        assertThat(response.cancelledFundings()).isEqualTo(3L);
        assertThat(response.totalPaymentAmount()).isEqualTo(4820000L);
        assertThat(response.pendingSettlements()).isEqualTo(7L);
        assertThat(response.pendingReports()).isZero();
    }
}
