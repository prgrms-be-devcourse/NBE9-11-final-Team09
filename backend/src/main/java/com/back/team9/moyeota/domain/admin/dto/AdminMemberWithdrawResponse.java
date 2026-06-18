package com.back.team9.moyeota.domain.admin.dto;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;

public record AdminMemberWithdrawResponse(
        Long memberId,
        MemberStatus status
) {
    public static AdminMemberWithdrawResponse from(Member member) {
        return new AdminMemberWithdrawResponse(
                member.getMemberId(),
                member.getStatus()
        );
    }
}