package com.back.team9.moyeota.domain.admin.dto;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;

import java.time.LocalDateTime;

public record AdminMemberDetailResponse(
        Long memberId,
        String email,
        String name,
        String nickname,
        String phoneNumber,
        Provider provider,
        String providerId,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long participationCount,
        long fundingCount,
        long paymentCount
) {
    public static AdminMemberDetailResponse of(
            Member member,
            long participationCount,
            long fundingCount,
            long paymentCount
    ) {
        return new AdminMemberDetailResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getProvider(),
                member.getProviderId(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                participationCount,
                fundingCount,
                paymentCount
        );
    }
}