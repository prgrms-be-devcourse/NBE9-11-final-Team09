package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.MemberLoginRequest;
import com.back.team9.moyeota.domain.member.dto.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberLoginService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public MemberLoginResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_LOGIN_CREDENTIALS
                ));

        if (member.getPassword() == null || !passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_LOGIN_CREDENTIALS
            );
        }

        validateMemberStatus(member);

        JwtTokenResponse tokens = jwtTokenProvider.createTokens(
                member.getMemberId()
        );

        return MemberLoginResponse.from(member, tokens);
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

        // 새로운 상태가 추가되더라도 명시적으로 허용하기 전에는 로그인 차단
        throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }
}