package com.back.team9.moyeota.global.jwt;

public record JwtAccessTokenInfo(
        Long memberId,
        String jti,
        long remainingExpiration
) {
}