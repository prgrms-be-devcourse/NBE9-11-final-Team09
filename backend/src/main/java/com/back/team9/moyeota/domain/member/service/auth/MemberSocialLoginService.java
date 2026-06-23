package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.dto.auth.MemberSocialLoginRequest;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.infrastructure.social.KakaoSocialLoginClient;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberSocialLoginService {

    private static final String DEFAULT_PHONE_NUMBER = "";
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final MemberRepository memberRepository;
    private final KakaoSocialLoginClient kakaoSocialLoginClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Transactional
    public MemberLoginResult login(MemberSocialLoginRequest request) {
        if (request.provider() != Provider.KAKAO) {
            throw new BusinessException(
                    ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER
            );
        }

        KakaoUserInfoResponse userInfo =
                kakaoSocialLoginClient.getUserInfo(request.accessToken());

        String providerId = String.valueOf(userInfo.id());

        Member member = memberRepository
                .findByProviderAndProviderId(request.provider(), providerId)
                .orElseGet(() -> createSocialMember(request, userInfo));

        validateMemberStatus(member);

        JwtTokenResponse tokens = jwtTokenProvider.createTokens(
                member.getMemberId()
        );

        return new MemberLoginResult(
                MemberLoginResponse.from(member, tokens),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresIn()
        );
    }

    private Member createSocialMember(
            MemberSocialLoginRequest request,
            KakaoUserInfoResponse userInfo
    ) {
        String email = normalizeEmail(userInfo.email());

        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    ErrorCode.SOCIAL_EMAIL_NOT_PROVIDED
            );
        }

        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String nickname = createUniqueNickname(userInfo);

        return memberRepository.save(Member.builder()
                .email(email)
                .password(null)
                .name(nickname)
                .nickname(nickname)
                .phoneNumber(DEFAULT_PHONE_NUMBER)
                .provider(request.provider())
                .providerId(String.valueOf(userInfo.id()))
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private String createUniqueNickname(KakaoUserInfoResponse userInfo) {
        String baseNickname = userInfo.nickname();

        if (baseNickname == null || baseNickname.isBlank()) {
            baseNickname = "kakao";
        }

        String suffix = "_" + userInfo.id();
        int maxBaseLength = MAX_NICKNAME_LENGTH - suffix.length();

        if (baseNickname.length() > maxBaseLength) {
            baseNickname = baseNickname.substring(0, maxBaseLength);
        }

        String nickname = baseNickname + suffix;

        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        return nickname;
    }

    private void validateMemberStatus(Member member) {
        if (member.getStatus() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_LOGIN_CREDENTIALS
            );
        }

        switch (member.getStatus()) {
            case ACTIVE -> {
                return;
            }
            case SUSPENDED ->
                    throw new BusinessException(ErrorCode.USER_SUSPENDED);
            case WITHDRAWN ->
                    throw new BusinessException(
                            ErrorCode.USER_ALREADY_WITHDRAWN
                    );
        }

        throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }
}