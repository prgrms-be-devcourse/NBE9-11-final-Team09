package com.back.team9.moyeota.global.jwt.dto;

import com.back.team9.moyeota.global.jwt.type.PrincipalType;

public record JwtAuthenticationInfo(
        Long principalId,
        PrincipalType principalType,
        String role,
        String jti
) {
}