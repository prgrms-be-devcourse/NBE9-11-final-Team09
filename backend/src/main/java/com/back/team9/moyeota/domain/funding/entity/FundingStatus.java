package com.back.team9.moyeota.domain.funding.entity;

public enum FundingStatus {
    RECRUITING, // 10일 전까지
    CONFIRMED, // 펀딩 성공 후 출발 24시간 전까지
    CLOSED, // 출발 24시간 전부터 출발시간 전까지
    COMPLETED, // 출발시간 이후
    FAILED, // 인원달성 실패
    CANCELLED // 취소됨
}
