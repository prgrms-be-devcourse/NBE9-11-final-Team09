package com.back.team9.moyeota.domain.settlement.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.settlement.dto.SettlementCreateRequest;
import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.domain.settlement.repository.SettlementRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final FundingRepository fundingRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Value("${platform.fee-rate}")
    private BigDecimal platformFeeRate;

    @Transactional
    public SettlementResponse create(SettlementCreateRequest request, Long memberId) {
        log.info("정산 생성 요청 (fundingId={}, memberId={})",
                request.fundingId(), memberId);

        Funding funding = fundingRepository.findById(request.fundingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        if (!funding.getMember().getMemberId().equals(memberId)) {
            log.warn("정산 접근 권한 없음 (fundingId={}, memberId={})",
                    request.fundingId(), memberId);
            throw new BusinessException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        if (funding.getStatus() != FundingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }
        if (settlementRepository.existsByFunding_FundingId(request.fundingId())) {
            log.warn("이미 정산 생성됨 (fundingId={})",
                    request.fundingId());
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        BigDecimal totalAmount = paymentRepository.sumAmountByFundingIdAndStatus(
                request.fundingId(), PaymentStatus.PAID);
        BigDecimal platformFee = totalAmount.multiply(platformFeeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal hostPaybackAmount = totalAmount.subtract(platformFee);

        Settlement settlement = Settlement.builder()
                .member(funding.getMember())
                .funding(funding)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .hostPaybackAmount(hostPaybackAmount)
                .status(SettlementStatus.CALCULATED)
                .paybackHold(funding.getPaybackHold())
                .build();

        Settlement saved;
        try {
            saved = settlementRepository.saveAndFlush(settlement);
            log.info("정산 생성 완료 (fundingId={}, settlementId={}, totalAmount={})",
                    request.fundingId(),
                    saved.getSettlementId(),
                    totalAmount);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }
        return SettlementResponse.from(saved);
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
        log.info("정산 승인 요청 (settlementId={})",
                settlementId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!settlement.getPaybackHold()) {
            log.warn("수동 승인 불필요한 정산 (settlementId={})", settlementId);
            throw new BusinessException(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED);
        }
        if (settlement.getStatus() != SettlementStatus.CALCULATED) {
            log.warn("정산 승인 불가 상태 (settlementId={}, status={})",
                    settlementId, settlement.getStatus());
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }

        settlement.approve(LocalDateTime.now(clock));
        log.info("정산 승인 완료 (settlementId={})",
                settlementId);
        return SettlementResponse.from(settlement);

    }

    @Transactional
    public SettlementResponse reject(Long settlementId) {
        log.info("정산 거절 요청 (settlementId={})",
                settlementId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!settlement.getPaybackHold()) {
            log.warn("수동 승인 불필요한 정산 (settlementId={})", settlementId);
            throw new BusinessException(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED);
        }
        if (settlement.getStatus() != SettlementStatus.CALCULATED) {
            log.warn("정산 거절 불가 상태 (settlementId={}, status={})",
                    settlementId, settlement.getStatus());
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }

        settlement.reject();
        log.info("정산 거절 완료 (settlementId={})",
                settlementId);
        return SettlementResponse.from(settlement);

    }

    @Transactional
    public void createByScheduler(Long fundingId) {
        log.info("스케줄러 정산 생성 시작 (fundingId={})", fundingId);

        if (settlementRepository.existsByFunding_FundingId(fundingId)) {
            log.info("이미 정산 생성됨 - 스킵 (fundingId={})", fundingId);
            return;
        }

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        BigDecimal totalAmount = paymentRepository.sumAmountByFundingIdAndStatus(
                fundingId, PaymentStatus.PAID);
        BigDecimal platformFee = totalAmount.multiply(platformFeeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal hostPaybackAmount = totalAmount.subtract(platformFee);

        LocalDateTime now = LocalDateTime.now(clock);
        boolean hold = funding.getPaybackHold();

        Settlement settlement = Settlement.builder()
                .member(funding.getMember())
                .funding(funding)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .hostPaybackAmount(hostPaybackAmount)
                .status(hold ? SettlementStatus.CALCULATED : SettlementStatus.COMPLETED)
                .paybackHold(hold)
                .paybackPaidAt(hold ? null : now)
                .build();

        settlementRepository.save(settlement);
        log.info("정산 생성 완료 (fundingId={}, status={}, totalAmount={})", fundingId, settlement.getStatus(), totalAmount);
    }


}
