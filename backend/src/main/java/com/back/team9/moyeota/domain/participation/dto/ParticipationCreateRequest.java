package com.back.team9.moyeota.domain.participation.dto;

import jakarta.validation.constraints.NotNull;

// 참여 신청 요청 DTO
public record ParticipationCreateRequest(
        // 참여할 펀딩 ID
        @NotNull(message = "펀딩 ID는 필수입니다.")
        Long fundingId,

        // 가는편 좌석 ID (필수 - 모든 펀딩은 가는편이 있어야 함)
        @NotNull(message = "가는편 좌석 ID는 필수입니다.")
        Long outboundSeatId,

        // 오는편 좌석 ID (편도인 경우 null)
        Long returnSeatId
) {

}
