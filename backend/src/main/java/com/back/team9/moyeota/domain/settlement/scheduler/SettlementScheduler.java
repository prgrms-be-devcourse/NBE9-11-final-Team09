package com.back.team9.moyeota.domain.settlement.scheduler;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.settlement.service.SettlementService;
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
public class SettlementScheduler {

    private final FundingRepository fundingRepository;
    private final SettlementService settlementService;
    private final Clock clock;

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void createSettlements() {
        LocalDate today = LocalDate.now(clock);
        List<Funding> targets = fundingRepository.findCompletedWithoutSettlement(today);

        if (targets.isEmpty()) return;

        log.info("정산 스케줄러 시작 — 대상: {}건", targets.size());
        for (Funding funding : targets) {
            try {
                settlementService.createByScheduler(funding.getFundingId());
            } catch (Exception e) {
                log.error("정산 생성 실패 — fundingId={}", funding.getFundingId(), e);
            }
        }
        log.info("정산 스케줄러 완료");
    }
}

