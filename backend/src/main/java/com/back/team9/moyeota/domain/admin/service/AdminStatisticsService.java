package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.AdminFundingStatistics;
import com.back.team9.moyeota.domain.admin.dto.AdminMemberStatistics;
import com.back.team9.moyeota.domain.admin.dto.AdminStatisticsResponse;
import com.back.team9.moyeota.domain.admin.repository.*;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final AdminMemberQueryRepository memberRepository;
    private final AdminFundingQueryRepository fundingRepository;
    private final AdminSettlementQueryRepository settlementRepository;
    private final AdminPaymentQueryRepository paymentRepository;

    @Transactional(readOnly = true)
    public AdminStatisticsResponse getStatistics() {
        AdminMemberStatistics memberStatistics =
                memberRepository.findStatistics(
                        MemberStatus.ACTIVE,
                        MemberStatus.WITHDRAWN
                );

        AdminFundingStatistics fundingStatistics =
                fundingRepository.findStatistics(
                        FundingStatus.RECRUITING,
                        FundingStatus.COMPLETED,
                        FundingStatus.CANCELLED
                );

        return new AdminStatisticsResponse(
                memberStatistics.totalUsers(),
                memberStatistics.activeUsers(),
                memberStatistics.withdrawnUsers(),
                fundingStatistics.activeFundings(),
                fundingStatistics.completedFundings(),
                fundingStatistics.cancelledFundings(),
                paymentRepository.sumAmountByStatus(PaymentStatus.PAID),
                settlementRepository.countByStatus(
                        SettlementStatus.CALCULATED
                ),
                0L
        );
    }
}