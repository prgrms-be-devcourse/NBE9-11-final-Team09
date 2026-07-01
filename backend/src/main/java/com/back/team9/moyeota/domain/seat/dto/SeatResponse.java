package com.back.team9.moyeota.domain.seat.dto;

import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatDisplayStatus;

// 좌석 단건 응답 DTO
public record SeatResponse(
        Long seatId,
        String seatNumber,
        SeatDisplayStatus status,
        boolean mySeat
) {

    public static SeatResponse from(
            Seat seat,
            SeatDisplayStatus status,
            boolean mySeat
    ) {
        return new SeatResponse(
                seat.getSeatId(),
                seat.getSeatNumber(),
                status,
                mySeat
        );
    }
}
