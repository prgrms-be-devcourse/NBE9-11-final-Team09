package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.service.FinalAmountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinalAmountScheduler {

    private final FundingRepository fundingRepository;
    private final FinalAmountService finalAmountService;
    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void setFinalAmounts() {
        LocalDate today = LocalDate.now(clock);
        LocalDate targetDate = today.plusDays(10);

        List<Funding> targets = fundingRepository.findByStatusAndDepartureDate(
                FundingStatus.CONFIRMED, targetDate);

        if (targets.isEmpty()) return;

        log.info("finalAmount 스케줄러 시작 — 대상: {}건", targets.size());
        for (Funding funding : targets) {
            try {
                finalAmountService.processFunding(funding.getFundingId());
            } catch (Exception e) {
                log.error("finalAmount 설정 실패 — fundingId={}", funding.getFundingId(), e);
            }
        }
        log.info("finalAmount 스케줄러 완료");
    }

}

