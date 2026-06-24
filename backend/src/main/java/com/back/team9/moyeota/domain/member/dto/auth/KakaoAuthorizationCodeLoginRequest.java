package com.back.team9.moyeota.domain.member.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record KakaoAuthorizationCodeLoginRequest(
        @NotBlank
        String code,

        @NotBlank
        String redirectUri
) {
}