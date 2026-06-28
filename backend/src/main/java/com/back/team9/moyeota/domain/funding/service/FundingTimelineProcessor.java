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
    private static final int RECRUITMENT_CLOSE_HOURS_BEFORE = 24;
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
                sendFundingNotification(funding, NotificationType.FUNDING_CONFIRMED);
                continue;
            }

            funding.fail();
            sendFundingNotification(funding, NotificationType.FUNDING_FAILED);
        }
    }

    // 가는 노선 출발 24시간 전부터 신규 참여 마감
    @Transactional
    public void closeRecruitment(LocalDateTime now) {
        LocalDateTime deadline =
                now.plusHours(RECRUITMENT_CLOSE_HOURS_BEFORE);

        List<Pathinfo> pathinfos =
                pathinfoRepository.findRecruitmentCloseTargets(
                        PathinfoStatus.PENDING,
                        Direction.OUTBOUND,
                        deadline,
                        FundingStatus.CONFIRMED
                );

        pathinfos.stream()
                .map(Pathinfo::getFunding)
                .distinct()
                .forEach(Funding::closeRecruitment);
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
                        FundingStatus.CLOSED
                );

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
        List<Pathinfo> pathinfos =
                pathinfoRepository.findPathinfosWithFunding(
                        PathinfoStatus.PENDING,
                        now,
                        FundingStatus.CLOSED
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
            log.error(
                    "펀딩 방장 알림 처리 실패 fundingId={}, notificationType={}",
                    fundingId,
                    type,
                    e
            );
        }

        try {
            notificationService.sendToFundingParticipants(
                    fundingId,
                    type
            );
        } catch (Exception e) {
            log.error(
                    "펀딩 알림 발송 실패 fundingId={}, notificationType={}",
                    fundingId,
                    type,
                    e
            );
        }
    }
}
