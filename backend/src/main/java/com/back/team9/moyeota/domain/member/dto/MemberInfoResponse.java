package com.back.team9.moyeota.domain.member.dto;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;

import java.time.LocalDateTime;

// 회원 정보 응답 DTO
public record MemberInfoResponse(
        Long memberId,
        String email,
        String name,
        String nickname,
        String phoneNumber,
        Provider provider,
        MemberStatus status,
        LocalDateTime createdAt
) {
    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getProvider(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }
}