package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoAuthorizationCodeLoginRequest;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoTokenResponse;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.dto.auth.MemberSocialLoginRequest;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.infrastructure.social.KakaoSocialLoginClient;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberSocialLoginService {

    private final KakaoSocialLoginClient kakaoSocialLoginClient;
    private final MemberSocialLoginTransactionService transactionService;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberLoginResult login(MemberSocialLoginRequest request) {
        if (request.provider() != Provider.KAKAO) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER
            );
        }

        KakaoUserInfoResponse userInfo =
                kakaoSocialLoginClient.getUserInfo(request.accessToken());

        Member member = transactionService.findOrCreateSocialMember(
                request.provider(),
                userInfo
        );

        return createLoginResult(member);
    }

    public MemberLoginResult loginWithKakaoAuthorizationCode(
            KakaoAuthorizationCodeLoginRequest request
    ) {
        KakaoTokenResponse tokenResponse = kakaoSocialLoginClient.getToken(
                request.code(),
                request.redirectUri()
        );

        KakaoUserInfoResponse userInfo = kakaoSocialLoginClient.getUserInfo(
                tokenResponse.accessToken()
        );

        Member member = transactionService.findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        );

        return createLoginResult(member);
    }

    private MemberLoginResult createLoginResult(Member member) {
        JwtTokenResponse tokens = jwtTokenProvider.createTokens(
                member.getMemberId()
        );

        return new MemberLoginResult(
                MemberLoginResponse.from(member, tokens),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresIn()
        );
    }
}