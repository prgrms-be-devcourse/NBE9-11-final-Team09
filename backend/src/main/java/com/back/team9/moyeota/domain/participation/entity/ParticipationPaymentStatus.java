package com.back.team9.moyeota.domain.participation.entity;

// 참여자의 결제 처리 상태
public enum ParticipationPaymentStatus {
    PENDING,   // 보증금 결제 전 (참여 생성 후 결제 대기 중)
    ACTIVE, //보증금 결제 완료
    CANCELED, //결제 취소 및 환불 완료
    COMPLETED, //모든 결제(보증금 + 잔액) 완료
    NO_SHOW //결제는 했지만 탑승하지 않음
}
