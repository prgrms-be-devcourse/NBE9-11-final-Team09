package com.back.team9.moyeota.domain.participation.dto;

import jakarta.validation.constraints.NotNull;

// 참여 신청 요청 DTO
public record ParticipationCreateRequest(
        @NotNull(message = "펀딩 ID는 필수입니다.")
        Long fundingId,

        @NotNull(message = "가는편 좌석 ID는 필수입니다.")
        Long outboundSeatId,
        Long returnSeatId
) {
}
