package com.back.team9.moyeota.domain.participation.scheduler;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoShowSingleProcessorTest {

    @Mock
    private ParticipationRepository participationRepository;

    private NoShowSingleProcessor noShowSingleProcessor;

    @BeforeEach
    void setUp() {
        noShowSingleProcessor = new NoShowSingleProcessor(participationRepository);
    }

    @Test
    @DisplayName("NO_SHOW 단건 처리 - 편도 ACTIVE 참여자 정상 처리")
    void processOneNoShow_편도ACTIVE참여자_정상처리() {
        // Given
        Long participationId = 1L;
        Seat outboundSeat = mock(Seat.class);

        Participation participation = mock(Participation.class);
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.ACTIVE);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(participationRepository.findById(participationId))
                .willReturn(Optional.of(participation));

        // When
        noShowSingleProcessor.processOneNoShow(participationId);

        // Then
        verify(participation).markAsNoShow();
        verify(outboundSeat).release();
    }

    @Test
    @DisplayName("NO_SHOW 단건 처리 - 왕복 ACTIVE 참여자 returnSeat도 복구")
    void processOneNoShow_왕복ACTIVE참여자_returnSeat도복구() {
        // Given
        Long participationId = 1L;
        Seat outboundSeat = mock(Seat.class);
        Seat returnSeat = mock(Seat.class);

        Participation participation = mock(Participation.class);
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.ACTIVE);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(returnSeat);

        given(participationRepository.findById(participationId))
                .willReturn(Optional.of(participation));

        // When
        noShowSingleProcessor.processOneNoShow(participationId);

        // Then
        verify(participation).markAsNoShow();
        verify(outboundSeat).release();
        verify(returnSeat).release();
    }

    @Test
    @DisplayName("NO_SHOW 단건 처리 - ACTIVE가 아니면 아무 처리 안 함 (잔액 결제 완료된 경우)")
    void processOneNoShow_ACTIVE아님_아무처리없음() {
        // Given
        Long participationId = 1L;

        Participation participation = mock(Participation.class);
        // paymentStatus = COMPLETED → 조건에서 바로 return되므로 getStatus()는 호출 안 됨!
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.COMPLETED);

        given(participationRepository.findById(participationId))
                .willReturn(Optional.of(participation));

        // When
        noShowSingleProcessor.processOneNoShow(participationId);

        // Then - 상태 변경 및 좌석 접근 모두 안 됨
        verify(participation, never()).markAsNoShow();
        verify(participation, never()).getOutboundSeat();
        verify(participation, never()).getReturnSeat();
    }

    @Test
    @DisplayName("NO_SHOW 단건 처리 - 존재하지 않는 참여자 PARTICIPATION_NOT_FOUND 예외 발생")
    void processOneNoShow_존재하지않는참여자_PARTICIPATION_NOT_FOUND예외() {
        // Given
        Long participationId = 999L;

        given(participationRepository.findById(participationId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noShowSingleProcessor.processOneNoShow(participationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND));
    }
}