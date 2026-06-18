package com.back.team9.moyeota.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminFundingCancelRequest(
        @NotBlank(message = "펀딩 강제 취소 사유는 필수입니다.")
        String reason
) {
}