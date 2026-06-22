package com.back.team9.moyeota.domain.member.infrastructure.redis;

public record EmailVerificationData(
        String verificationCodeHash
) {
}