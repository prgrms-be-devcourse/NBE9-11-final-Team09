package com.back.team9.moyeota.domain.member.dto.history;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 회원이 참여한 펀딩 정보 응답 DTO
public record MemberFundingResponse(
        Long fundingId,
        String fundingTitle,
        LocalDate departureDate,
        Long currentParticipants,
        Integer maxParticipants,
        FundingStatus status,
        LocalDateTime createdAt,
        List<String> hostSeatNumbers
) {
}