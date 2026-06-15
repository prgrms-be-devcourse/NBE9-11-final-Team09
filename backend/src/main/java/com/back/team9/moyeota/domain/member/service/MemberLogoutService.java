package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.jwt.TokenType;
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

        if (!jwtTokenProvider.validateToken(accessToken)
                || jwtTokenProvider.getTokenType(accessToken)
                != TokenType.ACCESS) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        jwtBlacklistService.blacklist(
                jwtTokenProvider.getJti(accessToken),
                jwtTokenProvider.getRemainingExpiration(accessToken)
        );
    }
}