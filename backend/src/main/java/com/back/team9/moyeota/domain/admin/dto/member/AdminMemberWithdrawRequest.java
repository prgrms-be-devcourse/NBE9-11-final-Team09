package com.back.team9.moyeota.domain.admin.dto.member;

import jakarta.validation.constraints.NotBlank;

public record AdminMemberWithdrawRequest(
        @NotBlank(message = "강제 탈퇴 사유는 필수입니다.")
        String reason
) {
}
