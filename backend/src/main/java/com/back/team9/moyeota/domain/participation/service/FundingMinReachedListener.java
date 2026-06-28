package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.event.FundingMinReachedEvent;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingMinReachedListener {

    private final NotificationService notificationService;
    private final ParticipationRepository participationRepository;

    @Async("refundExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMinReached(FundingMinReachedEvent event) {

        Long fundingId = event.fundingId();

        log.info("최소 인원 달성 처리 시작 (fundingId={})", fundingId);
        List<Long> memberIds = participationRepository.findMemberIdsByFundingIdAndPaymentStatusIn(
                fundingId,
                List.of(
                        ParticipationPaymentStatus.ACTIVE,
                        ParticipationPaymentStatus.COMPLETED
                )
        );
        try {
            notificationService.sendMimeMessage(
                    event.hostMemberId(),
                    fundingId,
                    NotificationType.MIN_REACHED
            );
        } catch (Exception e) {
            log.warn("최소 인원 달성 호스트 알림 발송 실패 (fundingId={})",
                    fundingId, e);
        }
        for (Long memberId : memberIds) {
            try {
                notificationService.sendMimeMessage(
                        memberId,
                        fundingId,
                        NotificationType.MIN_REACHED
                );
            } catch (Exception e) {
                log.warn("최소 인원 달성 참여자 알림 발송 실패 (fundingId={}, memberId={})",
                        fundingId, memberId, e);
            }
        }

        log.info("최소 인원 달성 처리 완료 (fundingId={}, 대상자수={})",
                fundingId,
                memberIds.size() + 1); // 방장 포함
    }
}