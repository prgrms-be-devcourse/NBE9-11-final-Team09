package com.back.team9.moyeota.domain.member.entity;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pending_member_signup")
public class PendingMemberSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pendingMemberSignupId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String encodedPassword;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String verificationCodeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private PendingMemberSignup(
            String email,
            String encodedPassword,
            String name,
            String nickname,
            String phoneNumber,
            String verificationCodeHash,
            LocalDateTime expiresAt
    ) {
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.verificationCodeHash = verificationCodeHash;
        this.expiresAt = expiresAt;
    }

    public static PendingMemberSignup create(
            String email,
            String encodedPassword,
            String name,
            String nickname,
            String phoneNumber,
            String verificationCodeHash,
            LocalDateTime expiresAt
    ) {
        return new PendingMemberSignup(
                email,
                encodedPassword,
                name,
                nickname,
                phoneNumber,
                verificationCodeHash,
                expiresAt
        );
    }

    public void validateVerificationCode(
            String verificationCode,
            PasswordEncoder passwordEncoder
    ) {
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!passwordEncoder.matches(
                verificationCode,
                verificationCodeHash
        )) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
    }

    public void update(
            String encodedPassword,
            String name,
            String nickname,
            String phoneNumber,
            String verificationCodeHash,
            LocalDateTime expiresAt
    ) {
        this.encodedPassword = encodedPassword;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.verificationCodeHash = verificationCodeHash;
        this.expiresAt = expiresAt;
    }

    public Member toMember() {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .nickname(nickname)
                .phoneNumber(phoneNumber)
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
