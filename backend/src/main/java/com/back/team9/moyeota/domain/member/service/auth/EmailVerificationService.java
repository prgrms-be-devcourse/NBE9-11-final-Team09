package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.infrastructure.mail.EmailVerificationMailSender;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationMailSender mailSender;

    public void sendVerificationCode(
            String email,
            String verificationCode
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[모여타] 회원가입 이메일 인증");
        message.setText(
                "인증코드는 " + verificationCode + " 입니다.\n"
                        + "인증코드는 30분 동안 유효합니다."
        );

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
