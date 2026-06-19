package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.entity.SendStatus;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final MemberRepository memberRepository;
    private final FundingRepository fundingRepository;
    private final NotificationRepository notificationRepository;

    private final MailService mailService;
    private final NotificationTemplateService templateService;

    public void sendMimeMessage(Long memberId,
                                Long fundingId,
                                NotificationType type) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

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
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        try {
            sendMailWithRetry(member.getEmail(), title, content);
            notification.markSuccess();

        } catch (Exception e) {
            notification.markFailed();
            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED);
        } finally {
            notificationRepository.save(notification);
        }
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
