package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FundingDetailResponse(
        Long fundingId,
        String title,
        String content,
        Long hostId,
        String hostNickname,
        LocalDate departureDate,
        FundingStatus status,
        BusType busType,
        Integer currentParticipants,
        Integer minParticipants,
        Integer maxParticipants,
        TripType tripType,
        List<PathInfoResponse> pathInfos,
        Long chatRoomId,
        Boolean isHost,
        Boolean isJoined,
        LocalDateTime createdAt
) {
}
