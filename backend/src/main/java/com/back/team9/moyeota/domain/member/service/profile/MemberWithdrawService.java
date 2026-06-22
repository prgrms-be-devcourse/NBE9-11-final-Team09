package com.back.team9.moyeota.domain.member.service.profile;

import com.back.team9.moyeota.domain.member.dto.profile.MemberWithdrawRequest;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberWithdrawService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public void withdraw(
            Long memberId,
            MemberWithdrawRequest request
    ) {
        Member member = getMember(memberId);

        validateActiveMember(member);
        validatePassword(member, request.password());

        member.withdraw(LocalDateTime.now(clock));
    }

    private Member getMember(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND
                ));
    }

    private void validateActiveMember(Member member) {
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validatePassword(
            Member member,
            String rawPassword
    ) {
        if (member.getPassword() == null || !passwordEncoder.matches(
                rawPassword,
                member.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_LOGIN_CREDENTIALS
            );
        }
    }
}
