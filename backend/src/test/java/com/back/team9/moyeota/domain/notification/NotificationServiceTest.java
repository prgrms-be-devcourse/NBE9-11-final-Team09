package com.back.team9.moyeota.domain.notification;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.notification.entity.Notification;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.domain.notification.service.MailService;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.notification.service.NotificationTemplateService;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import java.time.Clock;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.any;
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
    private MailService mailService;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private Clock clock;

    @Test
    void 알림_전체_흐름_성공() {
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
                .title("테스트 펀딩")
                .build();

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.of(funding));

        when(templateService.getSubject(any(), any()))
                .thenReturn("제목");

        when(templateService.buildContent(any(), any(), any()))
                .thenReturn("<html>content</html>");

        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // when
        notificationService.sendMimeMessage(
                memberId,
                fundingId,
                NotificationType.FUNDING_CONFIRMED
        );

        // then
        verify(mailService, times(1))
                .send(eq("test@test.com"), eq("제목"), eq("<html>content</html>"));

        verify(notificationRepository, atLeastOnce())
                .save(any(Notification.class));
    }
}