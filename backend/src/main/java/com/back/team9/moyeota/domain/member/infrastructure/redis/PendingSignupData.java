package com.back.team9.moyeota.domain.member.infrastructure.redis;

public record PendingSignupData(
        String email,
        String encodedPassword,
        String name,
        String nickname,
        String phoneNumber
) {
}