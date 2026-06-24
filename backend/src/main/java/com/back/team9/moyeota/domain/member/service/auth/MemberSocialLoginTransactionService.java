package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberSocialLoginTransactionService {

    private static final String DEFAULT_PHONE_NUMBER = "";
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final MemberRepository memberRepository;

    @Transactional
    public Member findOrCreateSocialMember(
            Provider provider,
            KakaoUserInfoResponse userInfo
    ) {
        String providerId = String.valueOf(userInfo.id());

        Member member = memberRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createSocialMember(provider, userInfo));

        validateMemberStatus(member);

        return member;
    }

    private Member createSocialMember(
            Provider provider,
            KakaoUserInfoResponse userInfo
    ) {
        String email = normalizeEmail(userInfo.email());

        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_NOT_PROVIDED);
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
                .provider(provider)
                .providerId(String.valueOf(userInfo.id()))
                .status(MemberStatus.ACTIVE)
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
        String nickname = createNickname(baseNickname, suffix, "");

        if (!memberRepository.existsByNickname(nickname)) {
            return nickname;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            String randomSuffix = "_" + UUID.randomUUID()
                    .toString()
                    .substring(0, 4);

            nickname = createNickname(baseNickname, suffix, randomSuffix);

            if (!memberRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }

        throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
    }

    private String createNickname(
            String baseNickname,
            String suffix,
            String randomSuffix
    ) {
        int maxBaseLength = MAX_NICKNAME_LENGTH
                - suffix.length()
                - randomSuffix.length();

        if (baseNickname.length() > maxBaseLength) {
            baseNickname = baseNickname.substring(
                    0,
                    Math.max(0, maxBaseLength)
            );
        }

        return baseNickname + randomSuffix + suffix;
    }

    private void validateMemberStatus(Member member) {
        if (member.getStatus() == null) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        switch (member.getStatus()) {
            case ACTIVE -> {
                return;
            }
            case SUSPENDED ->
                    throw new BusinessException(ErrorCode.USER_SUSPENDED);
            case WITHDRAWN ->
                    throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }
}