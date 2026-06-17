package com.back.team9.moyeota.domain.seat.service;

import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.seat.dto.SeatLayoutResponse;
import com.back.team9.moyeota.domain.seat.dto.SeatResponse;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatDisplayStatus;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatRedisService seatRedisService;

    @Mock
    private PathinfoRepository pathinfoRepository;

    @InjectMocks
    private SeatService seatService;

    // ==================== getSeatLayout 테스트 ====================

    @Test
    @DisplayName("좌석 배치도 조회 - 정상 조회 성공")
    void getSeatLayout_정상조회_좌석목록반환() {
        // Given
        Long pathId = 1L;
        Long currentMemberId = 1L;

        Pathinfo pathinfo = mock(Pathinfo.class);

        Seat seat = mock(Seat.class);
        given(seat.getSeatId()).willReturn(1L);
        given(seat.getSeatNumber()).willReturn("1A");
        given(seat.getStatus()).willReturn(SeatStatus.AVAILABLE);

        given(pathinfoRepository.findById(pathId)).willReturn(Optional.of(pathinfo));
        given(seatRepository.findByPathinfo_PathinfoId(pathId)).willReturn(List.of(seat));
        // MGET 결과: 홀딩 중인 좌석 없음
        given(seatRedisService.getHoldMemberIds(List.of(1L)))
                .willReturn(Collections.emptyMap());

        // When
        SeatLayoutResponse response = seatService.getSeatLayout(pathId, currentMemberId);

        // Then
        assertThat(response.pathId()).isEqualTo(pathId);
        assertThat(response.seats()).hasSize(1);
        assertThat(response.seats().get(0).status()).isEqualTo(SeatDisplayStatus.AVAILABLE);
        assertThat(response.seats().get(0).mySeat()).isFalse();
    }

    @Test
    @DisplayName("좌석 배치도 조회 - 존재하지 않는 노선 PATH_NOT_FOUND 예외 발생")
    void getSeatLayout_존재하지않는노선_PATH_NOT_FOUND예외() {
        // Given
        Long pathId = 999L;
        Long currentMemberId = 1L;

        given(pathinfoRepository.findById(pathId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatService.getSeatLayout(pathId, currentMemberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PATH_NOT_FOUND));
    }

    @Test
    @DisplayName("좌석 배치도 조회 - HOLD 중인 좌석 상태 HOLD로 반환")
    void getSeatLayout_홀딩중인좌석_HOLD상태반환() {
        // Given
        Long pathId = 1L;
        Long currentMemberId = 1L;
        Long holdMemberId = 2L; // 다른 유저가 선점 중

        Pathinfo pathinfo = mock(Pathinfo.class);

        Seat seat = mock(Seat.class);
        given(seat.getSeatId()).willReturn(1L);
        given(seat.getSeatNumber()).willReturn("1A");
        given(seat.getStatus()).willReturn(SeatStatus.AVAILABLE);

        given(pathinfoRepository.findById(pathId)).willReturn(Optional.of(pathinfo));
        given(seatRepository.findByPathinfo_PathinfoId(pathId)).willReturn(List.of(seat));
        // MGET 결과: 1번 좌석을 2번 유저가 선점 중
        given(seatRedisService.getHoldMemberIds(List.of(1L)))
                .willReturn(Map.of(1L, holdMemberId));

        // When
        SeatLayoutResponse response = seatService.getSeatLayout(pathId, currentMemberId);

        // Then
        assertThat(response.seats().get(0).status()).isEqualTo(SeatDisplayStatus.HOLD);
        assertThat(response.seats().get(0).mySeat()).isFalse(); // 내가 선점한 게 아님
    }

    @Test
    @DisplayName("좌석 배치도 조회 - 내가 선점한 좌석 mySeat true 반환")
    void getSeatLayout_내가선점한좌석_mySeat_true반환() {
        // Given
        Long pathId = 1L;
        Long currentMemberId = 1L;

        Pathinfo pathinfo = mock(Pathinfo.class);

        Seat seat = mock(Seat.class);
        given(seat.getSeatId()).willReturn(1L);
        given(seat.getSeatNumber()).willReturn("1A");
        given(seat.getStatus()).willReturn(SeatStatus.AVAILABLE);

        given(pathinfoRepository.findById(pathId)).willReturn(Optional.of(pathinfo));
        given(seatRepository.findByPathinfo_PathinfoId(pathId)).willReturn(List.of(seat));
        // MGET 결과: 1번 좌석을 내가 선점 중
        given(seatRedisService.getHoldMemberIds(List.of(1L)))
                .willReturn(Map.of(1L, currentMemberId));

        // When
        SeatLayoutResponse response = seatService.getSeatLayout(pathId, currentMemberId);

        // Then
        assertThat(response.seats().get(0).status()).isEqualTo(SeatDisplayStatus.HOLD);
        assertThat(response.seats().get(0).mySeat()).isTrue(); // 내가 선점한 좌석!
    }

    // ==================== holdSeat 테스트 ====================

    @Test
    @DisplayName("좌석 선점 - 정상 선점 성공")
    void holdSeat_정상선점_HOLD상태반환() {
        // Given
        Long seatId = 1L;
        Long currentMemberId = 1L;

        Pathinfo pathinfo = mock(Pathinfo.class);
        given(pathinfo.getStatus()).willReturn(PathinfoStatus.PENDING);

        Seat seat = mock(Seat.class);
        given(seat.getSeatId()).willReturn(seatId);
        given(seat.getSeatNumber()).willReturn("1A");
        given(seat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(seat.getPathinfo()).willReturn(pathinfo);

        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // When
        SeatResponse response = seatService.holdSeat(seatId, currentMemberId);

        // Then
        assertThat(response.status()).isEqualTo(SeatDisplayStatus.HOLD);
        assertThat(response.mySeat()).isTrue();
        verify(seatRedisService).holdSeat(seatId, currentMemberId);
    }

    @Test
    @DisplayName("좌석 선점 - 존재하지 않는 좌석 SEAT_NOT_FOUND 예외 발생")
    void holdSeat_존재하지않는좌석_SEAT_NOT_FOUND예외() {
        // Given
        Long seatId = 999L;
        Long currentMemberId = 1L;

        given(seatRepository.findById(seatId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> seatService.holdSeat(seatId, currentMemberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_NOT_FOUND));

        verify(seatRedisService, never()).holdSeat(seatId, currentMemberId);
    }

    @Test
    @DisplayName("좌석 선점 - 이미 BOOKED된 좌석 SEAT_ALREADY_OCCUPIED 예외 발생")
    void holdSeat_이미BOOKED된좌석_SEAT_ALREADY_OCCUPIED예외() {
        // Given
        Long seatId = 1L;
        Long currentMemberId = 1L;

        Pathinfo pathinfo = mock(Pathinfo.class);
        given(pathinfo.getStatus()).willReturn(PathinfoStatus.PENDING);

        Seat seat = mock(Seat.class);
        given(seat.getStatus()).willReturn(SeatStatus.BOOKED);
        given(seat.getPathinfo()).willReturn(pathinfo);

        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // When & Then
        assertThatThrownBy(() -> seatService.holdSeat(seatId, currentMemberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_ALREADY_OCCUPIED));

        verify(seatRedisService, never()).holdSeat(seatId, currentMemberId);
    }

    @Test
    @DisplayName("좌석 선점 - 완료된 노선 PATH_INVALID_STATUS 예외 발생")
    void holdSeat_완료된노선_PATH_INVALID_STATUS예외() {
        // Given
        Long seatId = 1L;
        Long currentMemberId = 1L;

        Pathinfo pathinfo = mock(Pathinfo.class);
        given(pathinfo.getStatus()).willReturn(PathinfoStatus.COMPLETED);

        Seat seat = mock(Seat.class);
        // seat.getStatus() 설정 제거 → 노선 상태 체크에서 이미 예외 발생하므로 불필요
        given(seat.getPathinfo()).willReturn(pathinfo);

        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // When & Then
        assertThatThrownBy(() -> seatService.holdSeat(seatId, currentMemberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PATH_INVALID_STATUS));

        verify(seatRedisService, never()).holdSeat(seatId, currentMemberId);
    }

    // TODO: PathinfoStatus에 CANCELLED 추가 후 아래 테스트 주석 해제
//    @Test
//    @DisplayName("좌석 선점 - 취소된 노선 PATH_INVALID_STATUS 예외 발생")
//    void holdSeat_취소된노선_PATH_INVALID_STATUS예외() {
//        // Given
//        Long seatId = 1L;
//        Long currentMemberId = 1L;
//
//        Pathinfo pathinfo = mock(Pathinfo.class);
//        given(pathinfo.getStatus()).willReturn(PathinfoStatus.CANCELLED);
//
//        Seat seat = mock(Seat.class);
//        given(seat.getPathinfo()).willReturn(pathinfo);
//
//        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));
//
//        assertThatThrownBy(() -> seatService.holdSeat(seatId, currentMemberId))
//                .isInstanceOf(BusinessException.class)
//                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
//                        .isEqualTo(ErrorCode.PATH_INVALID_STATUS));
//
//        verify(seatRedisService, never()).holdSeat(seatId, currentMemberId);
//    }
}