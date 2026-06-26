package com.back.team9.moyeota.domain.admin.dto.auth;

import com.back.team9.moyeota.domain.admin.entity.Admin;
import com.back.team9.moyeota.global.jwt.dto.JwtAccessTokenResponse;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        long accessTokenExpiresIn,
        AdminResponse admin
) {
    public static AdminLoginResponse from(
            Admin admin,
            JwtAccessTokenResponse token
    ) {
        return new AdminLoginResponse(
                token.accessToken(),
                "Bearer",
                token.expiresIn(),
                new AdminResponse(
                        admin.getAdminId(),
                        admin.getLoginId(),
                        admin.getRole().name()
                )
        );
    }

    public record AdminResponse(
            Long adminId,
            String loginId,
            String role
    ) {
    }
}
