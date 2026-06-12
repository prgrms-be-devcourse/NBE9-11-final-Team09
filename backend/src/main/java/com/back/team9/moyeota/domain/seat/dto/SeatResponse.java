package com.back.team9.moyeota.domain.seat.dto;

import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatDisplayStatus;

// 좌석 단건 응답 DTO
public record SeatResponse(
        Long seatId, // 좌석 ID
        String seatNumber, // 좌석 번호 (예: 1A, 2B)
        SeatDisplayStatus status, // 화면 표시용 상태 (AVAILABLE / HOLD / BOOKED)
        boolean mySeat  // 현재 로그인한 사용자가 선점한 좌석이면 true, 아니면 false
) {
    // Seat Entity → SeatResponse 변환
    public static SeatResponse from(Seat seat, SeatDisplayStatus status, boolean mySeat) {
        return new SeatResponse(
                seat.getSeatId(),
                seat.getSeatNumber(),
                status,
                mySeat
        );
    }
}
