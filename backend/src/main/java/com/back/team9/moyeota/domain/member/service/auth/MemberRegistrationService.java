package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupData;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional
    public void register(PendingSignupData signupData) {
        validateDuplicates(signupData);

        memberRepository.save(Member.builder()
                .email(signupData.email())
                .password(signupData.encodedPassword())
                .name(signupData.name())
                .nickname(signupData.nickname())
                .phoneNumber(signupData.phoneNumber())
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }

    private void validateDuplicates(PendingSignupData signupData) {
        if (memberRepository.existsByEmail(signupData.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(signupData.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
