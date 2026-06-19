package com.back.team9.moyeota.domain.member.dto.auth;

public record MemberLoginResult(
        MemberLoginResponse response,
        String refreshToken,
        long refreshTokenExpiresIn
) {
}