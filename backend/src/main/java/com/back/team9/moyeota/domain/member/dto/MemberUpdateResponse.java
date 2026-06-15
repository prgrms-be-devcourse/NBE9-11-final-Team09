package com.back.team9.moyeota.domain.member.dto;

import com.back.team9.moyeota.domain.member.entity.Member;

import java.time.LocalDateTime;

// 회원 정보 업데이트 응답 DTO
public record MemberUpdateResponse(
        Long memberId,
        String email,
        String name,
        String nickname,
        String phoneNumber,
        LocalDateTime updatedAt
) {
    public static MemberUpdateResponse from(Member member) {
        return new MemberUpdateResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getUpdatedAt()
        );
    }
}