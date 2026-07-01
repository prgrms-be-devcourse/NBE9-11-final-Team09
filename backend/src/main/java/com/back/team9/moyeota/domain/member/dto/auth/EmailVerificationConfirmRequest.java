package com.back.team9.moyeota.domain.member.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
        @NotBlank String email,
        @NotBlank String verificationCode
) {
}