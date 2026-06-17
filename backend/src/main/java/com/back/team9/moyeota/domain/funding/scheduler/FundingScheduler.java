package com.back.team9.moyeota.domain.funding.scheduler;


import com.back.team9.moyeota.domain.funding.service.FundingTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class FundingScheduler {

    private final FundingTimelineService fundingTimelineService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul") // 1시간마다 조회
    public void processFundingTimeline() {
        log.info("펀딩 타임라인 스케줄러 시작");

        try {
            fundingTimelineService.processTimeline();
            log.info("펀딩 타임라인 스케줄러 완료");
        } catch (Exception e) {
            log.error("펀딩 타임라인 스케줄러 실패", e);
        }
    }
}
