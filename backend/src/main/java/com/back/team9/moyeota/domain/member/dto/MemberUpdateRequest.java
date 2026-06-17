package com.back.team9.moyeota.domain.member.dto;

import jakarta.validation.constraints.Pattern;

// 회원 정보 업데이트 요청 DTO
public record MemberUpdateRequest(
        String nickname,

        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {
}