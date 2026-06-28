package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.notification.dto.NotificationResponse;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.entity.SendStatus;
import com.back.team9.moyeota.domain.notification.event.NotificationCreatedEvent;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final MemberRepository memberRepository;
    private final FundingRepository fundingRepository;
    private final NotificationRepository notificationRepository;
    private final ParticipationRepository participationRepository;
    private final NotificationAsyncService notificationAsyncService;
    private final ApplicationEventPublisher eventPublisher;

    private final NotificationTemplateService templateService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendMimeMessage(Long memberId,
                                Long fundingId,
                                NotificationType type) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        sendMessage(member, funding, type);
    }

    // 방장용 알림
    @Transactional
    public void sendToFundingHost(
            Long memberId,
            Long fundingId,
            NotificationType type
    ) {
        if (isAlreadySent(memberId, fundingId, type)) {
            return;
        }

        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            Funding funding = fundingRepository.findById(fundingId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

            sendMessage(member, funding, type);
        } catch (Exception e) {
            log.error(
                    "펀딩 방장 알림 발송 실패 memberId={}, fundingId={}, notificationType={}",
                    memberId,
                    fundingId,
                    type,
                    e
            );
        }
    }

    // 참가자용 알림
    @Transactional
    public void sendToFundingParticipants(
            Long fundingId,
            NotificationType type
    ) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        List<Long> memberIds = participationRepository.findMemberIdsByFundingIdAndStatus(
                fundingId,
                ParticipationStatus.ACTIVE
        );

        if (memberIds.isEmpty()) {
            return;
        }

        Set<Long> sentMemberIds = new HashSet<>(
                notificationRepository.findSentMemberIds(
                        fundingId,
                        type,
                        memberIds
                )
        );

        List<Member> members = memberRepository.findAllById(memberIds);

        for (Member member : members) {
            if (sentMemberIds.contains(member.getMemberId())) {
                continue;
            }

            try {
                sendMessage(member, funding, type);
            } catch (Exception e) {
                log.error(
                        "펀딩 참가자 알림 발송 실패 memberId={}, fundingId={}, notificationType={}",
                        member.getMemberId(),
                        fundingId,
                        type,
                        e
                );
            }
        }
    }

    private void sendMessage(
            Member member,
            Funding funding,
            NotificationType type
    ) {
        String title = templateService.getSubject(type, funding.getTitle());
        String content = templateService.buildContent(
                type,
                member.getNickname(),
                funding.getTitle()
        );

        Notification notification = Notification.builder()
                .member(member)
                .funding(funding)
                .notificationType(type)
                .title(title)
                .content(content)
                .sendStatus(SendStatus.PENDING)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        eventPublisher.publishEvent(
                new NotificationCreatedEvent(
                        savedNotification.getNotificationId()
                )
        );
    }

    private boolean isAlreadySent(
            Long memberId,
            Long fundingId,
            NotificationType type
    ) {
        return notificationRepository.existsByMember_MemberIdAndFunding_FundingIdAndNotificationType(
                memberId,
                fundingId,
                type
        );
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMessages(
            Long memberId,
            Pageable pageable
    ) {
        return notificationRepository
                .findAllWithFunding(memberId, pageable)
                .map(NotificationResponse::from);
    }
}
