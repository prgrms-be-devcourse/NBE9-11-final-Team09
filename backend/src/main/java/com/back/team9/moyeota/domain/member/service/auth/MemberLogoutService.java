package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.blacklist.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.dto.JwtAccessTokenInfo;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.resolver.JwtTokenResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLogoutService {

    private final JwtTokenResolver jwtTokenResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;

    public void logout(String authorization) {
        String accessToken = jwtTokenResolver.findToken(authorization)
                .orElseThrow(() -> {
                    log.warn("로그아웃 실패 - 토큰 없음");
                    return new BusinessException(ErrorCode.TOKEN_INVALID);
                });

        JwtAccessTokenInfo tokenInfo = jwtTokenProvider
                .findAccessTokenInfo(accessToken)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TOKEN_INVALID
                ));

        jwtBlacklistService.blacklist(
                tokenInfo.jti(),
                tokenInfo.remainingExpiration()
        );
        log.info("로그아웃 완료 (jti={})", tokenInfo.jti());
    }
}
