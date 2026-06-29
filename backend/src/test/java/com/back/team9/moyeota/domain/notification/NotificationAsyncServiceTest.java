package com.back.team9.moyeota.domain.notification;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.SendStatus;
import com.back.team9.moyeota.domain.notification.event.NotificationCreatedEvent;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.notification.service.NotificationAsyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAsyncServiceTest {

    @InjectMocks
    private NotificationAsyncService notificationAsyncService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MailService mailService;

    @Mock
    private Clock clock;

    @Test
    void 메일_발송_성공() {

        // given
        Member member = Member.builder()
                .email("test@test.com")
                .build();

        Notification notification = Notification.builder()
                .notificationId(1L)
                .member(member)
                .title("제목")
                .content("내용")
                .sendStatus(SendStatus.PENDING)
                .build();

        when(notificationRepository.findByIdWithMember(1L))
                .thenReturn(Optional.of(notification));

        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // when
        notificationAsyncService.sendMail(
                new NotificationCreatedEvent(1L)
        );

        // then
        verify(mailService, times(1))
                .send("test@test.com", "제목", "내용");

        assertThat(notification.getSendStatus())
                .isEqualTo(SendStatus.SUCCESS);

        assertThat(notification.getEmailSentAt())
                .isNotNull();
    }

    @Test
    void 메일_발송_실패하면_FAILED_상태() {

        // given
        Member member = Member.builder()
                .email("test@test.com")
                .build();

        Notification notification = Notification.builder()
                .notificationId(1L)
                .member(member)
                .title("제목")
                .content("내용")
                .sendStatus(SendStatus.PENDING)
                .build();

        when(notificationRepository.findByIdWithMember(1L))
                .thenReturn(Optional.of(notification));

        doThrow(new RuntimeException())
                .when(mailService)
                .send(any(), any(), any());

        // when
        notificationAsyncService.sendMail(
                new NotificationCreatedEvent(1L)
        );

        // then
        verify(mailService, times(3)) // retry 반영
                .send(any(), any(), any());

        assertThat(notification.getSendStatus())
                .isEqualTo(SendStatus.FAILED);
    }

    @Test
    void 메일_재시도_후_성공() {

        // given
        Member member = Member.builder()
                .email("test@test.com")
                .build();

        Notification notification = Notification.builder()
                .notificationId(1L)
                .member(member)
                .title("제목")
                .content("내용")
                .sendStatus(SendStatus.PENDING)
                .build();

        when(notificationRepository.findByIdWithMember(1L))
                .thenReturn(Optional.of(notification));

        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doNothing()
                .when(mailService)
                .send(any(), any(), any());

        // when
        notificationAsyncService.sendMail(
                new NotificationCreatedEvent(1L)
        );

        // then
        verify(mailService, times(3))
                .send(any(), any(), any());

        assertThat(notification.getSendStatus())
                .isEqualTo(SendStatus.SUCCESS);
    }
}