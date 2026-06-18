package com.back.team9.moyeota.domain.admin.dto;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;

import java.time.LocalDateTime;

public record AdminMemberListResponse(
        Long memberId,
        String email,
        String name,
        String nickname,
        String phoneNumber,
        Provider provider,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminMemberListResponse from(Member member) {
        return new AdminMemberListResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getProvider(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}