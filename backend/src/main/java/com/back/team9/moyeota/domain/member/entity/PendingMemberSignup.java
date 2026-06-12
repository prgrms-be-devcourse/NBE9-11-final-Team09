package com.back.team9.moyeota.domain.member.entity;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * 이메일 인증 전 회원가입 정보를 임시 저장하는 회원 도메인 내부 엔티티
 * 인증 완료 시 Member로 변환 후 삭제되며 다른 도메인에서는 참조하지 않는 엔티티
 * 추후 Redis 도입 시 삭제 예정
 */
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
