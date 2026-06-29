package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.infrastructure.mail.EmailVerificationMailSender;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import jakarta.mail.MessagingException;
//import org.springframework.mail.MailException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(
            String email,
            String verificationCode
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[모여타] 회원가입 이메일 인증");
            helper.setText(buildHtml(verificationCode), true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String buildHtml(String verificationCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>[모여타] 회원가입 이메일 인증 안내</title>
                </head>
                <body style="margin:0;padding:0;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;background-color:#f9fafb;-webkit-font-smoothing:antialiased;">
                
                <table align="center"
                       border="0"
                       cellpadding="0"
                       cellspacing="0"
                       width="100%%"
                       style="max-width:550px;margin:40px auto;background-color:#ffffff;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,.05);overflow:hidden;border:1px solid #e5e7eb;">

                    <tr>
                        <td style="padding:32px 40px 20px 40px;border-bottom:1px solid #f3f4f6;">
                            <h1 style="margin:0;font-size:24px;font-weight:700;color:#1f2937;">
                                모여타<span style="color:#2563eb;">.</span>
                            </h1>
                        </td>
                    </tr>

                    <tr>
                        <td style="padding:40px;">
                            <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#374151;font-weight:bold;">
                                안녕하세요, 모여타를 방문해 주셔서 감사합니다.
                            </p>

                            <p style="margin:0 0 32px;font-size:14px;line-height:1.6;color:#4b5563;">
                                본인 확인 및 서비스 이용을 위해 아래의 인증 코드를 입력 화면에 입력해 주세요.
                            </p>

                            <table border="0"
                                   cellpadding="0"
                                   cellspacing="0"
                                   width="100%%"
                                   style="background-color:#f3f4f6;border-radius:8px;text-align:center;">
                                <tr>
                                    <td style="padding:20px;font-size:32px;font-weight:700;letter-spacing:6px;color:#2563eb;">
                                        %s
                                    </td>
                                </tr>
                            </table>

                            <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#9ca3af;text-align:center;">
                                * 이 인증 코드는 발송 후 <strong>30분간</strong>만 유효합니다.
                            </p>
                        </td>
                    </tr>

                    <tr>
                        <td style="padding:24px 40px;background-color:#f9fafb;border-top:1px solid #f3f4f6;">
                            <p style="margin:0 0 4px;font-size:12px;line-height:1.5;color:#9ca3af;">
                                본 메일은 발신 전용 메일이므로 회신 주셔도 답변되지 않습니다.
                            </p>

                            <p style="margin:0;font-size:12px;line-height:1.5;color:#9ca3af;">
                                &copy; 2026 모여타 Team 09. All rights reserved.
                            </p>
                        </td>
                    </tr>

                </table>

                </body>
                </html>
                """.formatted(verificationCode);
    }
}
