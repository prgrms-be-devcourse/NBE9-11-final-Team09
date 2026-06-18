package com.back.team9.moyeota.domain.participation.event;

// 참여 취소 시 발행되는 이벤트
// Payment 도메인에서 환불 처리 시 사용
public record ParticipationCancelledEvent(
        Long participationId // 취소된 참여 ID
) {
}
