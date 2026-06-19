package com.back.team9.moyeota.domain.member.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record MemberSignupRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String nickname,
        @NotBlank String phoneNumber
) {
}