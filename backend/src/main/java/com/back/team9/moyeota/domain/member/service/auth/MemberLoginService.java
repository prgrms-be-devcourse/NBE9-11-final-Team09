package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginRequest;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.dto.JwtTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLoginService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public MemberLoginResult login(MemberLoginRequest request) {
        log.info("로그인 요청 (email={})", maskEmail(request.email()));

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_LOGIN_CREDENTIALS
                ));

        if (member.getPassword() == null || !passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )) {
            log.warn("로그인 실패 (email={})", maskEmail(request.email()));
            throw new BusinessException(
                    ErrorCode.INVALID_LOGIN_CREDENTIALS
            );
        }

        validateMemberStatus(member);

        JwtTokenResponse tokens = jwtTokenProvider.createTokens(
                member.getMemberId()
        );

        log.info("로그인 성공 (memberId={})", member.getMemberId());

        return new MemberLoginResult(
                MemberLoginResponse.from(member, tokens),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresIn()
        );
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
            case SUSPENDED -> {
                log.warn("정지 회원 로그인 시도 (memberId={})", member.getMemberId());
                throw new BusinessException(ErrorCode.USER_SUSPENDED);
            }
            case WITHDRAWN -> {
                log.warn("탈퇴 회원 로그인 시도 (memberId={})", member.getMemberId());
                throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
            }
        }

        // 새로운 상태가 추가되더라도 명시적으로 허용하기 전에는 로그인 차단
        throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "null";
        }
        return email.replaceAll("(?<=.{3}).(?=.*@)", "*");
    }
}
