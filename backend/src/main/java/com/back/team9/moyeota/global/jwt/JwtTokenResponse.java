package com.back.team9.moyeota.global.jwt;

public record JwtTokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}