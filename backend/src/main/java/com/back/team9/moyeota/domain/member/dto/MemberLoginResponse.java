package com.back.team9.moyeota.domain.member.dto;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.global.jwt.JwtTokenResponse;

public record MemberLoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        UserResponse user
) {

    public static MemberLoginResponse from(
            Member member,
            JwtTokenResponse tokens
    ) {
        return new MemberLoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.accessTokenExpiresIn(),
                tokens.refreshTokenExpiresIn(),
                UserResponse.from(member)
        );
    }

    public record UserResponse(
            Long userId,
            String email,
            String name,
            String nickname
    ) {
        public static UserResponse from(Member member) {
            return new UserResponse(
                    member.getMemberId(),
                    member.getEmail(),
                    member.getName(),
                    member.getNickname()
            );
        }
    }
}