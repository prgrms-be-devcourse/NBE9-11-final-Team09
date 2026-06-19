package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLogoutService {

    private final JwtTokenResolver jwtTokenResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;

    public void logout(String authorization) {
        String accessToken = jwtTokenResolver.findToken(authorization)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOKEN_INVALID
                ));

        JwtAccessTokenInfo tokenInfo = jwtTokenProvider
                .findAccessTokenInfo(accessToken)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOKEN_INVALID
                ));

        jwtBlacklistService.blacklist(
                tokenInfo.jti(),
                tokenInfo.remainingExpiration()
        );
    }
}
