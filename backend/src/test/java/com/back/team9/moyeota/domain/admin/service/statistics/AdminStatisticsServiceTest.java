package com.back.team9.moyeota.domain.admin.service.statistics;

import com.back.team9.moyeota.domain.admin.dto.statistics.AdminFundingStatistics;
import com.back.team9.moyeota.domain.admin.dto.statistics.AdminMemberStatistics;
import com.back.team9.moyeota.domain.admin.dto.statistics.AdminStatisticsResponse;
import com.back.team9.moyeota.domain.admin.repository.funding.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.member.AdminMemberQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.payment.AdminPaymentQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.settlement.AdminSettlementQueryRepository;
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
        AdminMemberStatistics memberStatistics =
                new AdminMemberStatistics(100L, 90L, 5L);
        AdminFundingStatistics fundingStatistics =
                new AdminFundingStatistics(10L, 20L, 3L);

        when(memberRepository.findStatistics(
                MemberStatus.ACTIVE,
                MemberStatus.WITHDRAWN
        )).thenReturn(memberStatistics);
        when(fundingRepository.findStatistics(
                FundingStatus.RECRUITING,
                FundingStatus.COMPLETED,
                FundingStatus.CANCELLED
        )).thenReturn(fundingStatistics);
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
