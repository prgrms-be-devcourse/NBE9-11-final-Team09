package com.back.team9.moyeota.domain.member.dto;

import com.back.team9.moyeota.domain.member.entity.PendingMemberSignup;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record MemberSignupRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String nickname,
        @NotBlank String phoneNumber
) {

    public PendingMemberSignup toEntity(
            String encodedPassword,
            String verificationCodeHash,
            LocalDateTime expiresAt
    ) {
        return PendingMemberSignup.create(
                email,
                encodedPassword,
                name,
                nickname,
                phoneNumber,
                verificationCodeHash,
                expiresAt
        );
    }
}