package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoAuthorizationCodeLoginRequest;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoTokenResponse;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.infrastructure.social.KakaoSocialLoginClient;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.dto.JwtTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSocialLoginService {

    private final KakaoSocialLoginClient kakaoSocialLoginClient;
    private final MemberSocialLoginTransactionService transactionService;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberLoginResult loginWithKakaoAuthorizationCode(
            KakaoAuthorizationCodeLoginRequest request
    ) {
        log.info("소셜 로그인 요청 (provider={})", Provider.KAKAO);
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

        log.info("소셜 로그인 성공 (memberId={}, provider={})",
                member.getMemberId(), Provider.KAKAO);

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