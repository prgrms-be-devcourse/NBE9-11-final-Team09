package com.back.team9.moyeota.domain.member.dto.auth;

import com.back.team9.moyeota.domain.member.entity.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberSocialLoginRequest(
        @NotNull
        Provider provider,

        @NotBlank
        String accessToken
) {
}