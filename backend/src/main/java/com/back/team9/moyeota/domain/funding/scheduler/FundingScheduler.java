package com.back.team9.moyeota.domain.funding.scheduler;


import com.back.team9.moyeota.domain.funding.service.FundingTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FundingScheduler {

    private final FundingTimelineService fundingTimelineService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul") // 1시간마다 조회
    public void processFundingTimeline() {
        fundingTimelineService.processTimeline();
    }
}
