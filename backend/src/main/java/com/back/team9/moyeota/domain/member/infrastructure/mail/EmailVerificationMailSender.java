package com.back.team9.moyeota.domain.member.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailVerificationMailSender {

    private final JavaMailSender mailSender;

    @Retryable(
            includes = MailException.class,
            maxRetriesString =
                    "${app.mail.retry.max-retries:2}",
            delayString =
                    "${app.mail.retry.delay:500}",
            multiplierString =
                    "${app.mail.retry.multiplier:2.0}",
            maxDelayString =
                    "${app.mail.retry.max-delay:2000}"
    )
    public void send(SimpleMailMessage message) {
        mailSender.send(message);
    }
}