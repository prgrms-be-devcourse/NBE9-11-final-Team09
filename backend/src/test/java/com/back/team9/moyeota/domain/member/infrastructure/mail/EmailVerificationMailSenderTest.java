package com.back.team9.moyeota.domain.member.infrastructure.mail;

import com.back.team9.moyeota.global.config.ResilienceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {
        ResilienceConfig.class,
        EmailVerificationMailSender.class
})
@TestPropertySource(properties = {
        "app.mail.retry.max-retries=2",
        "app.mail.retry.delay=1",
        "app.mail.retry.multiplier=1.0",
        "app.mail.retry.max-delay=10"
})
@DisplayName("이메일 인증 메일 재시도 테스트")
class EmailVerificationMailSenderTest {

    @Autowired
    private EmailVerificationMailSender mailSender;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    @DisplayName("메일 발송이 두 번 실패한 후 세 번째에 성공한다")
    void sendFailsTwiceThenSucceeds() {
        MailSendException exception = new MailSendException("SMTP 오류");

        doThrow(exception)
                .doThrow(exception)
                .doNothing()
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        mailSender.send(createMessage());

        verify(javaMailSender, times(3))
                .send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("메일 발송이 계속 실패하면 총 세 번 시도 후 예외를 던진다")
    void sendContinuesToFail() {
        doThrow(new MailSendException("SMTP 오류"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> mailSender.send(createMessage()))
                .isInstanceOf(MailSendException.class);

        verify(javaMailSender, times(3))
                .send(any(SimpleMailMessage.class));
    }

    private SimpleMailMessage createMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("member@example.com");
        message.setSubject("인증번호");
        message.setText("인증번호: A1B2C3");
        return message;
    }
}