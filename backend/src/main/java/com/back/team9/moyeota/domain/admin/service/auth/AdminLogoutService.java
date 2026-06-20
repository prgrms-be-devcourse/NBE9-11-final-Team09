package com.back.team9.moyeota.domain.admin.service.auth;

import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.jwt.JwtAuthenticationInfo;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminLogoutService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;

    public void logout(String accessToken) {
        JwtAuthenticationInfo authenticationInfo =
                jwtTokenProvider.findAuthenticationInfo(accessToken)
                        .filter(info ->
                                info.principalType() == PrincipalType.ADMIN
                        )
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.TOKEN_INVALID)
                        );

        long remainingExpiration =
                jwtTokenProvider.getRemainingExpiration(accessToken);

        if (remainingExpiration <= 0) {
            return;
        }

        jwtBlacklistService.blacklist(
                authenticationInfo.jti(),
                remainingExpiration
        );
    }
}