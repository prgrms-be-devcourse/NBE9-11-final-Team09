package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalAmountService {

    private final FundingRepository fundingRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public void processFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        List<Participation> participations = participationRepository
                .findByFunding_FundingIdAndStatus(fundingId, ParticipationStatus.ACTIVE);

        if (participations.isEmpty()) return;

        boolean alreadySet = participations.stream()
                .allMatch(p -> p.getFinalAmount().compareTo(BigDecimal.ZERO) > 0);
        if (alreadySet) return;

        BigDecimal finalAmount = funding.getTotalPrice()
                .divide(BigDecimal.valueOf(participations.size()), 0, RoundingMode.CEILING);

        participations.forEach(p -> p.updateFinalAmount(finalAmount));
        log.info("finalAmount 설정 완료 — fundingId={}, 인원={}, finalAmount={}",
                fundingId, participations.size(), finalAmount);
    }
}
