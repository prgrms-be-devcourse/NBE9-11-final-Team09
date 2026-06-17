package com.back.team9.moyeota.domain.admin.service;

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
        return new AdminStatisticsResponse(
                memberRepository.count(),
                memberRepository.countByStatus(MemberStatus.ACTIVE),
                memberRepository.countByStatus(MemberStatus.WITHDRAWN),
                fundingRepository.countByStatus(FundingStatus.RECRUITING),
                fundingRepository.countByStatus(FundingStatus.COMPLETED),
                fundingRepository.countByStatus(FundingStatus.CANCELLED),
                paymentRepository.sumAmountByStatus(PaymentStatus.PAID),
                settlementRepository.countByStatus(SettlementStatus.CALCULATED),
                0L
        );
    }
}