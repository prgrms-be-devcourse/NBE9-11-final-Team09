package com.back.team9.moyeota.domain.seat.dto;

import java.util.List;

// 좌석 배치도 전체 응답 DTO
public record SeatLayoutResponse(
        Long pathId,
        String busType,
        List<SeatResponse> seats
) {

    // SeatLayoutResponse 생성
    public static SeatLayoutResponse from(
            Long pathId,
            String busType,
            List<SeatResponse> seats
    ) {
        return new SeatLayoutResponse(
                pathId,
                busType,
                seats
        );
    }
}
