package com.back.team9.moyeota.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void updateProfile(
            String nickname,
            String phoneNumber,
            LocalDateTime updatedAt
    ) {
        if (nickname != null) {
            this.nickname = nickname;
        }

        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }

        this.updatedAt = updatedAt;
    }

    public void withdraw(LocalDateTime updatedAt) {
        this.status = MemberStatus.WITHDRAWN;
        this.updatedAt = updatedAt;
    }
}
