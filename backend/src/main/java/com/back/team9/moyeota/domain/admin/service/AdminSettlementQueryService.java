package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.*;
import com.back.team9.moyeota.domain.admin.repository.AdminPaymentQueryRepository;
import com.back.team9.moyeota.domain.admin.repository.AdminSettlementQueryRepository;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettlementQueryService {

    private final AdminSettlementQueryRepository settlementRepository;
    private final AdminPaymentQueryRepository paymentRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminSettlementListResponse> getSettlements(
            Pageable pageable
    ) {
        return PageResponse.from(
                settlementRepository.findAll(pageable)
                        .map(AdminSettlementListResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public AdminSettlementDetailResponse getSettlement(Long settlementId) {
        Settlement settlement = settlementRepository
                .findBySettlementId(settlementId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SETTLEMENT_NOT_FOUND
                ));

        Long fundingId = settlement.getFunding().getFundingId();

        AdminSettlementPaymentSummaryResponse paymentSummary =
                new AdminSettlementPaymentSummaryResponse(
                        paymentRepository.countByParticipation_Funding_FundingIdAndStatus(
                                fundingId,
                                PaymentStatus.PAID
                        ),
                        paymentRepository.countByParticipation_Funding_FundingIdAndPaymentTypeAndStatus(
                                fundingId,
                                PaymentType.DEPOSIT,
                                PaymentStatus.PAID
                        ),
                        paymentRepository.countByParticipation_Funding_FundingIdAndPaymentTypeAndStatus(
                                fundingId,
                                PaymentType.BALANCE,
                                PaymentStatus.PAID
                        ),
                        paymentRepository.sumAmountByFundingIdAndStatus(
                                fundingId,
                                PaymentStatus.PAID
                        )
                );

        return AdminSettlementDetailResponse.of(settlement, paymentSummary);
    }
}