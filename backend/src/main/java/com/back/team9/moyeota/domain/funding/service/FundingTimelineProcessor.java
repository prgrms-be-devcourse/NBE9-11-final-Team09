package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FundingTimelineProcessor {

    // 펀딩 확정 기준 날짜 (현재 10일)
    private static final int CONFIRMATION_DAYS_BEFORE_DEPARTURE = 10;
    private static final int DEPARTURE_REMINDER_HOURS_BEFORE = 2;

    private final FundingRepository fundingRepository;
    private final PathinfoRepository pathinfoRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationService notificationService;

    // 펀딩 성공 또는 실패 상태변경
    @Transactional
    public void confirmOrFailFundings(LocalDate today) {
        LocalDate targetDepartureDate =
                today.plusDays(CONFIRMATION_DAYS_BEFORE_DEPARTURE);
        log.info("펀딩 상태 변경 시작 (targetDate={})", targetDepartureDate);

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
                log.info("펀딩 확정 (fundingId={})",
                        funding.getFundingId());
                sendFundingNotification(funding, NotificationType.FUNDING_CONFIRMED);
                continue;
            }

            funding.fail();
            log.info("펀딩 실패 (fundingId={})",
                    funding.getFundingId());
            sendFundingNotification(funding, NotificationType.FUNDING_FAILED);
        }
    }

    // 출발 n 시간 전 알림
    public void sendDepartureReminders(LocalDateTime now) {
        LocalDateTime reminderStart =
                now.plusHours(DEPARTURE_REMINDER_HOURS_BEFORE);
        LocalDateTime reminderEnd =
                reminderStart.plusHours(1);

        List<Pathinfo> pathinfos =
                pathinfoRepository.findDepartureReminderTargets(
                        PathinfoStatus.PENDING,
                        Direction.OUTBOUND,
                        reminderStart,
                        reminderEnd,
                        FundingStatus.CONFIRMED
                );
        log.info("출발 알림 대상 조회 완료 (count={})",
                pathinfos.size());

        for (Pathinfo pathinfo : pathinfos) {
            sendFundingNotification(
                    pathinfo.getFunding(),
                    NotificationType.DEPARTURE_REMINDER
            );
        }

        // 노선 출발 2시간 전 알림 필요시 추가
    }

    // 출발시간 지난 노선과 연결된 펀딩 완료(메일 전송)
    @Transactional
    public void completePathinfosAndFundings(LocalDateTime now) {
        log.info("노선/펀딩 완료 처리 시작");
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
            log.info("완료 처리 대상 펀딩 없음");
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
                log.info("펀딩 완료 (fundingId={})",
                        funding.getFundingId());
            }
        }
        log.info("노선/펀딩 완료 처리 완료 (처리수={})", fundingIds.size());
    }

    private void sendFundingNotification(
            Funding funding,
            NotificationType type
    ) {
        Long fundingId = funding.getFundingId();
        Long hostId = funding.getMember().getMemberId();

        try {
            notificationService.sendToFundingHost(
                    hostId,
                    fundingId,
                    type
            );
        } catch (Exception e) {
            log.error("펀딩 방장 알림 발송 실패 (fundingId={}, notificationType={})",
                    fundingId, type, e);
        }

        try {
            notificationService.sendToFundingParticipants(
                    fundingId,
                    type
            );
        } catch (Exception e) {
            log.error("펀딩 참여자 알림 발송 실패 (fundingId={}, notificationType={})",
                    fundingId, type, e);
        }
    }
}
