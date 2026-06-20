package com.back.team9.moyeota.global.jwt;

public record JwtAccessTokenResponse(
        String accessToken,
        long expiresIn
) {
}