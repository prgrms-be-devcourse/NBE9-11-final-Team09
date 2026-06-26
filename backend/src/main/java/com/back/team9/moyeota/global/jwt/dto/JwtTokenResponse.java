package com.back.team9.moyeota.global.jwt.dto;

public record JwtTokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}