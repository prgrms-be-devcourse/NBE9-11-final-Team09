package com.back.team9.moyeota.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberWithdrawRequest(
        @NotBlank
        String password
) {
}