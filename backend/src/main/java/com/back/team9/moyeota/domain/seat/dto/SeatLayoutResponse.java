package com.back.team9.moyeota.domain.seat.dto;

import java.util.List;

// 좌석 배치도 전체 응답 DTO
public record SeatLayoutResponse(
        Long pathId, // 노선 ID
        String busType, // 버스 종류 (예: BUS_25, BUS_45)
        List<SeatResponse> seats // 해당 노선의 전체 좌석 목록
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
