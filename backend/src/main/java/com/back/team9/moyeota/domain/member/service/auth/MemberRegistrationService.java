package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupData;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private final MemberRepository memberRepository;

    @Transactional
    public void register(PendingSignupData signupData) {
        log.info("회원가입 요청 (email={})", maskEmail(signupData.email()));
        validateDuplicates(signupData);

        Member savedMember = memberRepository.save(
                Member.builder()
                        .email(signupData.email())
                        .password(signupData.encodedPassword())
                        .name(signupData.name())
                        .nickname(signupData.nickname())
                        .phoneNumber(signupData.phoneNumber())
                        .status(MemberStatus.ACTIVE)
                        .build()
        );

        log.info("회원가입 완료 (memberId={}, email={})",
                savedMember.getMemberId(),
                maskEmail(signupData.email()));
    }

    private void validateDuplicates(PendingSignupData signupData) {
        if (memberRepository.existsByEmail(signupData.email())) {
            log.warn("회원가입 실패 - 이메일 중복 (email={})",
                    maskEmail(signupData.email()));
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(signupData.nickname())) {
            log.warn("회원가입 실패 - 닉네임 중복 (nickname={})",
                    signupData.nickname());
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "null";
        }
        return email.replaceAll("(?<=.{3}).(?=.*@)", "*");
    }
}
