package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.infrastructure.mail.EmailVerificationMailSender;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 인증 서비스 테스트")
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationMailSender mailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("인증코드를 포함한 이메일을 발송한다")
    void sendVerificationCodeSuccessfully() {
        // Given
        String email = "member@example.com";
        String verificationCode = "A1B2C3";
        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When
        emailVerificationService.sendVerificationCode(
                email,
                verificationCode
        );

        // Then
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly(email);
        assertThat(message.getSubject()).isNotBlank();
        assertThat(message.getText()).contains(verificationCode);
    }

    @Test
    @DisplayName("메일 발송 재시도가 모두 실패하면 이메일 발송 실패 예외를 반환한다")
    void sendVerificationCodeWhenMailSendingFails() {
        // Given
        doThrow(new MailSendException("SMTP 오류"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // When & Then
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
