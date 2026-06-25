package com.back.team9.moyeota.domain.notification.dto;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse (
        Long notificationId, //알림 아이디
        Long fundingId,  //펀딩
        NotificationType notificationType,  //알람 타입
        String title, //알림 제목
        String content, //알림 내용
        LocalDateTime emailSentAt //보낸 시간
){
    public static NotificationResponse from (Notification notification){
        return new NotificationResponse(
                notification.getNotificationId(),
            notification.getFunding().getFundingId(),
            notification.getNotificationType(),
            notification.getTitle(),
            notification.getContent(),
            notification.getEmailSentAt()
        );
    }
}
