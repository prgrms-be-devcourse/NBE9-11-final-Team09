package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
public class FundingTimelineProcessor {

    // 펀딩 확정 기준 날짜 (현재 10일)
    private static final int CONFIRMATION_DAYS_BEFORE_DEPARTURE = 10;

    private final FundingRepository fundingRepository;
    private final PathinfoRepository pathinfoRepository;
    private final ParticipationRepository participationRepository;

    // 펀딩 성공 또는 실패 상태변경
    @Transactional
    public void confirmOrFailFundings(LocalDate today) {
        LocalDate targetDepartureDate =
                today.plusDays(CONFIRMATION_DAYS_BEFORE_DEPARTURE);

        List<Funding> fundings =
                fundingRepository.findByStatusAndDepartureDateLessThanEqual(
                        FundingStatus.RECRUITING,
                        targetDepartureDate
                );

        if (fundings.isEmpty()) {
            return;
        }

        List<Long> fundingIds = fundings.stream()
                .map(Funding::getFundingId)
                .toList();

        Map<Long, Long> activeParticipantCountMap =
                participationRepository.countByFundingIdsAndStatus(
                                fundingIds,
                                ParticipationStatus.ACTIVE
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                ParticipationRepository.FundingParticipationCount::getFundingId,
                                ParticipationRepository.FundingParticipationCount::getCount
                        ));

        for (Funding funding : fundings) {
            long activeParticipants =
                    activeParticipantCountMap.getOrDefault(funding.getFundingId(), 0L);

            if (activeParticipants >= funding.getMinParticipants()) {
                funding.confirm();
                continue;
            }

            funding.fail();
        }
    }

    // 출발시간 지난 노선과 연결된 펀딩 완료
    @Transactional
    public void completePathinfosAndFundings(LocalDateTime now) {
        List<Pathinfo> pathinfos =
                pathinfoRepository.findPathinfosWithFunding(
                        PathinfoStatus.PENDING,
                        now,
                        FundingStatus.CONFIRMED
                );

        pathinfos.forEach(Pathinfo::complete);

        List<Long> fundingIds = pathinfos.stream()
                .map(path -> path.getFunding().getFundingId())
                .distinct()
                .toList();

        if (fundingIds.isEmpty()) {
            return;
        }

        List<Pathinfo> allPathinfos =
                pathinfoRepository.findByFunding_FundingIdInAndStatusNot(
                        fundingIds,
                        PathinfoStatus.CANCELLED
                );

        Map<Long, List<Pathinfo>> pathinfosByFundingId = allPathinfos.stream()
                .collect(groupingBy(path -> path.getFunding().getFundingId()));

        for (Pathinfo pathinfo : pathinfos) {
            Funding funding = pathinfo.getFunding();

            boolean allCompleted = pathinfosByFundingId
                    .getOrDefault(funding.getFundingId(), Collections.emptyList())
                    .stream()
                    .allMatch(path -> path.getStatus() == PathinfoStatus.COMPLETED);

            if (allCompleted) {
                funding.complete();
            }
        }
    }
}
