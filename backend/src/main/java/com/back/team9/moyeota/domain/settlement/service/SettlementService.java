package com.back.team9.moyeota.domain.settlement.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.settlement.dto.SettlementCreateRequest;
import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.domain.settlement.repository.SettlementRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final FundingRepository fundingRepository;
    private final Clock clock;

    @Value("${platform.fee-rate}")
    private BigDecimal platformFeeRate;

    @Transactional
    public SettlementResponse create(SettlementCreateRequest request, Long memberId) {

        Funding funding = fundingRepository.findById(request.fundingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        if (!funding.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        if (funding.getStatus() != FundingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }
        if (settlementRepository.existsByFunding_FundingId(request.fundingId())) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        BigDecimal platformFee = request.totalAmount().multiply(platformFeeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal hostPaybackAmount = request.totalAmount().subtract(platformFee);

        Settlement settlement = Settlement.builder()
                .member(funding.getMember())
                .funding(funding)
                .totalAmount(request.totalAmount())
                .platformFee(platformFee)
                .hostPaybackAmount(hostPaybackAmount)
                .status(SettlementStatus.CALCULATED)
                .paybackHold(funding.getPaybackHold())
                .createdAt(LocalDateTime.now(clock))
                .build();

        Settlement saved;
        try {
            saved = settlementRepository.save(settlement);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }        return SettlementResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public SettlementResponse getByFundingId(Long fundingId, Long memberId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));
        if (!funding.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return SettlementResponse.from(
                settlementRepository.findByFunding_FundingId(fundingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND))
        );
    }

    @Transactional
    public SettlementResponse approve(Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!settlement.getPaybackHold()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED);
        }
        if (settlement.getStatus() != SettlementStatus.CALCULATED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }

        settlement.approve();
        return SettlementResponse.from(settlement);

    }

    @Transactional
    public SettlementResponse reject(Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!settlement.getPaybackHold()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED);
        }
        if (settlement.getStatus() != SettlementStatus.CALCULATED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }

        settlement.reject();
        return SettlementResponse.from(settlement);

    }


}
