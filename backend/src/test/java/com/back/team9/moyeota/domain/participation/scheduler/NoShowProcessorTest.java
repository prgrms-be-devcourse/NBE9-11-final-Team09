package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoShowProcessorTest {

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private NoShowSingleProcessor noShowSingleProcessor;

    private NoShowProcessor noShowProcessor;

    private static final LocalDateTime NOW = LocalDateTime.of(2027, 6, 20, 9, 0);

    @BeforeEach
    void setUp() {
        noShowProcessor = new NoShowProcessor(participationRepository, noShowSingleProcessor);
    }

    @Test
    @DisplayName("NO_SHOW 처리 - 정상 처리 성공")
    void processNoShow_정상처리_성공() {
        // Given
        Participation participation = mock(Participation.class);
        given(participation.getParticipationId()).willReturn(1L);

        given(participationRepository.findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participation));

        // When
        noShowProcessor.processNoShow(NOW);

        // Then
        verify(participationRepository).findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        );
        verify(noShowSingleProcessor).processOneNoShow(1L);
    }

    @Test
    @DisplayName("NO_SHOW 처리 - 대상 없으면 아무것도 처리하지 않음")
    void processNoShow_대상없음_아무처리없음() {
        // Given
        given(participationRepository.findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        )).willReturn(List.of());

        // When
        noShowProcessor.processNoShow(NOW);

        // Then
        verify(participationRepository).findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        );
        verifyNoMoreInteractions(participationRepository);
        verifyNoInteractions(noShowSingleProcessor);
    }

    @Test
    @DisplayName("NO_SHOW 처리 - 한 명 실패해도 나머지 계속 처리")
    void processNoShow_한명실패_나머지계속처리() {
        // Given
        Participation participation1 = mock(Participation.class);
        given(participation1.getParticipationId()).willReturn(1L);

        Participation participation2 = mock(Participation.class);
        given(participation2.getParticipationId()).willReturn(2L);

        given(participationRepository.findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participation1, participation2));

        // 1번 처리 시 예외 발생
        doThrow(new RuntimeException("처리 실패"))
                .when(noShowSingleProcessor).processOneNoShow(1L);

        // When
        noShowProcessor.processNoShow(NOW);

        // Then - 1번 실패해도 2번은 정상 처리
        verify(noShowSingleProcessor).processOneNoShow(1L);
        verify(noShowSingleProcessor).processOneNoShow(2L);
    }
}