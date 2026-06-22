package com.back.team9.moyeota.domain.member.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank String email
) {
}