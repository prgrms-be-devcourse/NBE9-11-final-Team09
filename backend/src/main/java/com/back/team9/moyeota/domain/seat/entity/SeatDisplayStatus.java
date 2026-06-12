package com.back.team9.moyeota.domain.seat.entity;

//좌석 상태 Enum - 응답(Response DTO) 전용
public enum SeatDisplayStatus {
    AVAILABLE, //선택 가능한 빈 좌석
    HOLD, //누군가 선점(홀딩) 중인 좌석
    BOOKED //결제 완료된 확정 좌석
}
