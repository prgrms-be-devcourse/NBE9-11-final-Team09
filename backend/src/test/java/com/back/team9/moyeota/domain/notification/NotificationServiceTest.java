package com.back.team9.moyeota.domain.notification;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.entity.SendStatus;
import com.back.team9.moyeota.domain.notification.event.NotificationCreatedEvent;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.notification.service.NotificationTemplateService;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void 알림_생성_후_이벤트를_발행() {

        // given
        Long memberId = 1L;
        Long fundingId = 10L;

        Member member = Member.builder()
                .memberId(memberId)
                .email("test@test.com")
                .nickname("tester")
                .build();

        Funding funding = Funding.builder()
                .fundingId(fundingId)
                .title("테스트")
                .build();

        Notification savedNotification = Notification.builder()
                .notificationId(100L) // ⭐ 핵심: ID 포함된 mock 반환 객체
                .member(member)
                .funding(funding)
                .notificationType(NotificationType.FUNDING_CONFIRMED)
                .title("제목")
                .content("내용")
                .sendStatus(SendStatus.PENDING)
                .build();

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.of(funding));

        when(templateService.getSubject(any(), any()))
                .thenReturn("제목");

        when(templateService.buildContent(any(), any(), any()))
                .thenReturn("내용");

        // ⭐ 핵심 수정: save 결과를 ID 포함 객체로 반환
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(savedNotification);

        // when
        notificationService.sendMimeMessage(
                memberId,
                fundingId,
                NotificationType.FUNDING_CONFIRMED
        );

        // then
        verify(notificationRepository).save(any(Notification.class));

        ArgumentCaptor<NotificationCreatedEvent> captor =
                ArgumentCaptor.forClass(NotificationCreatedEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        NotificationCreatedEvent event = captor.getValue();

        assertThat(event.getNotificationId())
                .isEqualTo(100L);
    }
}