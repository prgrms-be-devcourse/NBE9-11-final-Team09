package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
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

    private NoShowProcessor noShowProcessor;

    private static final LocalDateTime NOW = LocalDateTime.of(2027, 6, 20, 9, 0);

    @BeforeEach
    void setUp() {
        noShowProcessor = new NoShowProcessor(participationRepository);
    }

    @Test
    @DisplayName("NO_SHOW 처리 - 편도 참여자 정상 처리 성공")
    void processNoShow_편도참여자_정상처리_성공() {
        // Given
        Seat outboundSeat = mock(Seat.class);

        Participation participation = mock(Participation.class);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null); // 편도

        given(participationRepository.findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participation));

        // When
        noShowProcessor.processNoShow(NOW);

        // Then
        verify(participation).markAsNoShow();
        verify(outboundSeat).release();
        verifyNoMoreInteractions(outboundSeat);
    }

    @Test
    @DisplayName("NO_SHOW 처리 - 왕복 참여자 정상 처리 성공 (returnSeat도 복구)")
    void processNoShow_왕복참여자_정상처리_성공() {
        // Given
        Seat outboundSeat = mock(Seat.class);
        Seat returnSeat = mock(Seat.class);

        Participation participation = mock(Participation.class);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(returnSeat); // 왕복

        given(participationRepository.findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participation));

        // When
        noShowProcessor.processNoShow(NOW);

        // Then
        verify(participation).markAsNoShow();
        verify(outboundSeat).release();
        verify(returnSeat).release();
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

        // Then - findNoShowTargets만 호출되고 그 이후 처리 없음
        verify(participationRepository).findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        );
        verifyNoMoreInteractions(participationRepository);
    }

    @Test
    @DisplayName("NO_SHOW 처리 - 한 명 실패해도 나머지 계속 처리")
    void processNoShow_한명실패_나머지계속처리() {
        // Given
        Participation participation1 = mock(Participation.class);
        given(participation1.getParticipationId()).willReturn(1L); // log.error에서 호출됨
        doThrow(new RuntimeException("처리 실패"))
                .when(participation1).markAsNoShow();

        Seat outboundSeat2 = mock(Seat.class);
        Participation participation2 = mock(Participation.class);
        given(participation2.getParticipationId()).willReturn(2L); // 일관성
        given(participation2.getOutboundSeat()).willReturn(outboundSeat2);
        given(participation2.getReturnSeat()).willReturn(null);

        given(participationRepository.findNoShowTargets(
                NOW,
                NOW.plusHours(24),
                ParticipationPaymentStatus.ACTIVE,
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participation1, participation2));

        // When
        noShowProcessor.processNoShow(NOW);

        // Then
        verify(participation1).markAsNoShow();
        verify(participation2).markAsNoShow();
        verify(outboundSeat2).release();
    }
}