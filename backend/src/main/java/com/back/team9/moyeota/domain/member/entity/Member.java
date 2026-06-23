package com.back.team9.moyeota.domain.member.entity;

import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {
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

    public void updateProfile(
            String nickname,
            String phoneNumber
    ) {
        if (nickname != null) {
            this.nickname = nickname;
        }

        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }

    public boolean isLocalMember() {
        return provider == null;
    }

    public boolean isSocialMember() {
        return provider != null;
    }
}
