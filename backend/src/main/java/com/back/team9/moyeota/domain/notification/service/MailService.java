package com.back.team9.moyeota.domain.notification.service;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender javaMailSender;

    public void send(String to, String subject, String content) {
        log.info("메일 발송 요청 (to={})", maskEmail(to));

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(mimeMessage);

            log.info("메일 발송 완료 (to={})", maskEmail(to));

        } catch (Exception e) {
            log.error("메일 발송 실패 (to={}, subject={})",
                    maskEmail(to), subject, e);

            throw new BusinessException(ErrorCode.NOTIFICATION_SEND_FAILED);
        }
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "null";
        }
        return email.replaceAll("(?<=.{3}).(?=.*@)", "*");
    }
}
