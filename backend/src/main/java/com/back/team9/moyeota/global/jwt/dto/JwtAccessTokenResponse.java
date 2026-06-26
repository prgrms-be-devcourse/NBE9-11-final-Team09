package com.back.team9.moyeota.global.jwt.dto;

public record JwtAccessTokenResponse(
        String accessToken,
        long expiresIn
) {
}