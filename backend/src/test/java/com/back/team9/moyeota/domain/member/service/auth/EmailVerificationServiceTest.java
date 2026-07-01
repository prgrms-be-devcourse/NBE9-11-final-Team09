package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.infrastructure.mail.EmailVerificationMailSender;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import jakarta.mail.internet.MimeMessage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 인증 서비스 테스트")
class EmailVerificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void sendVerificationCodeSuccessfully() {
        // given
        MimeMessage mimeMessage = mock(MimeMessage.class);

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        // when
        emailVerificationService.sendVerificationCode(
                "member@example.com",
                "A1B2C3"
        );

        // then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("메일 발송 재시도가 모두 실패하면 이메일 발송 실패 예외를 반환한다")
    void sendVerificationCodeWhenMailSendingFails() {
        // given
        MimeMessage mimeMessage = mock(MimeMessage.class);

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        doThrow(new MailSendException("SMTP 오류"))
                .when(mailSender)
                .send(mimeMessage);

        // when & then
        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationCode(
                        "member@example.com",
                        "A1B2C3"
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.EMAIL_SEND_FAILED)
                );
    }
}
