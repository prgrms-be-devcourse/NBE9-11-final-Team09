package com.back.team9.moyeota.domain.notification.entity;

public enum NotificationType {
    MIN_REACHED, //최소인원 달성
    FUNDING_CONFIRMED, //펀딩 확정
    PAYMENT_DEADLINE, //데드라인
    FUNDING_FAILED, //펀딩 실패
    DEPARTURE_REMINDER, //출발 일정
    EMERGENCY_NOTICE, //긴급 알람
    PAYMENT_COMPLETED //결제 완료
}
