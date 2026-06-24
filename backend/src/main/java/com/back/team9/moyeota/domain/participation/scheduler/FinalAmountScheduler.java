package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinalAmountScheduler {

    private final FundingRepository fundingRepository;
    private final ParticipationRepository participationRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void setFinalAmounts() {
        LocalDate today = LocalDate.now(clock);
        LocalDate targetDate = today.plusDays(10);

        List<Funding> targets = fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, targetDate);

        if (targets.isEmpty()) return;

        log.info("finalAmount 스케줄러 시작 — 대상: {}건", targets.size());
        for (Funding funding : targets) {
            try {
                processFunding(funding);
            } catch (Exception e) {
                log.error("finalAmount 설정 실패 — fundingId={}", funding.getFundingId(), e);
            }
        }
        log.info("finalAmount 스케줄러 완료");
    }

    private void processFunding(Funding funding) {
        List<Participation> participations = participationRepository
                .findByFunding_FundingIdAndStatus(
                        funding.getFundingId(), ParticipationStatus.ACTIVE);

        if (participations.isEmpty()) return;

        // 멱등성: 이미 설정된 경우 skip
        boolean alreadySet = participations.stream()
                .anyMatch(p -> p.getFinalAmount().compareTo(BigDecimal.ZERO) > 0);
        if (alreadySet) return;

        BigDecimal finalAmount = funding.getTotalPrice()
                .divide(BigDecimal.valueOf(participations.size()), 0, RoundingMode.CEILING);

        participations.forEach(p -> p.updateFinalAmount(finalAmount));
        log.info("finalAmount 설정 완료 — fundingId={}, 인원={}, finalAmount={}",
                funding.getFundingId(), participations.size(), finalAmount);
    }
}

