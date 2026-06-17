package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FundingTimelineService {

    // 펀딩 확정 기준 날짜 (현재 10일)
    private static final int CONFIRMATION_DAYS_BEFORE_DEPARTURE = 10;

    private final Clock clock;
    private final FundingRepository fundingRepository;
    private final PathinfoRepository pathinfoRepository;
    private final ParticipationRepository participationRepository;
    private final FundingTimelineProcessor processor;

    @Transactional
    public void processTimeline() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        processor.confirmOrFailFundings(today);
        processor.completePathinfosAndFundings(now);
    }

}
