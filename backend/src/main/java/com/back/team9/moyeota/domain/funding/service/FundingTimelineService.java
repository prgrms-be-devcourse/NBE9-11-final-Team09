package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.participation.scheduler.NoShowProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FundingTimelineService {

    private final Clock clock;
    private final FundingTimelineProcessor processor;
    private final NoShowProcessor noShowProcessor;

    public void processTimeline() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        processor.confirmOrFailFundings(today);
        noShowProcessor.processNoShow(now);
        processor.completePathinfosAndFundings(now);
    }

}
