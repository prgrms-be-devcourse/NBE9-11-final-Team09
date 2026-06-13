package com.back.team9.moyeota.domain.member.dto;

public record MemberLoginResult(
        MemberLoginResponse response,
        String refreshToken,
        long refreshTokenExpiresIn
) {
}