package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateService {

    public String getSubject(NotificationType type, String fundingTitle) {
        return fundingTitle + " : " + switch (type) {
            case MIN_REACHED -> "최소 인원 달성";
            case FUNDING_CONFIRMED -> "펀딩 확정";
            case PAYMENT_DEADLINE -> "결제 마감 임박";
            case FUNDING_FAILED -> "펀딩 실패";
            case DEPARTURE_REMINDER -> "출발 알림";
            case EMERGENCY_NOTICE -> "긴급 공지";
            case PAYMENT_COMPLETED -> "결제 완료";
        };
    }

    public String buildContent(NotificationType type, String nickname, String title) {

        String safeNickname = org.springframework.web.util.HtmlUtils.htmlEscape(nickname);
        String safeTitle = org.springframework.web.util.HtmlUtils.htmlEscape(title);

        return switch (type) {

            case MIN_REACHED -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 최소 인원이 달성되었습니다 🎉</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);

            case FUNDING_CONFIRMED -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 펀딩이 확정되었습니다 🎉</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);

            case PAYMENT_DEADLINE -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 결제 마감이 임박했습니다 ⚠</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);

            case FUNDING_FAILED -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 펀딩이 실패했습니다 😢</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);

            case DEPARTURE_REMINDER -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 출발 일정이 다가왔습니다 🚍</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);

            case EMERGENCY_NOTICE -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 긴급 공지입니다 🚨</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);

            case PAYMENT_COMPLETED -> """
                    <html>
                    <body>
                        <h2>%s님</h2>
                        <p>[%s] 결제가 완료되었습니다 ✅</p>
                    </body>
                    </html>
                    """.formatted(safeNickname, safeTitle);
        };
    }
}