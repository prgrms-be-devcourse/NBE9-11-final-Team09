package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.entity.SendStatus;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private final Clock clock;

    private final MailService mailService;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToFundingHost(
            Long memberId,
            Long fundingId,
            NotificationType type
    ) {
        if (isAlreadySent(memberId, fundingId, type)) {
            return;
        }

        sendMimeMessage(memberId, fundingId, type);
    }

    // 참가자용 알림
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        notificationRepository.save(notification);

        try {
            sendMailWithRetry(member.getEmail(), title, content);
            notification.markSuccess(LocalDateTime.now(clock));

        } catch (Exception e) {
            notification.markFailed();
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED);
        } finally {
            notificationRepository.save(notification);
        }
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

    private void sendMailWithRetry(
            String email,
            String title,
            String content
    ) {
        int retryCount = 0;

        while (retryCount < 3) {
            try {
                mailService.send(email, title, content);
                return;

            } catch (Exception e) {
                retryCount++;

                log.warn("메일 발송 실패 ({} / 3)", retryCount);

                if (retryCount >= 3) {
                    throw e;
                }
            }
        }
    }
}
