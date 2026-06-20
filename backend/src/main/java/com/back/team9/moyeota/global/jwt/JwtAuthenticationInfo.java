package com.back.team9.moyeota.global.jwt;

public record JwtAuthenticationInfo(
        Long principalId,
        PrincipalType principalType,
        String role,
        String jti
) {
}