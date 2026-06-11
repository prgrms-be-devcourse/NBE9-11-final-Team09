package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
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
        Integer totalPrice,
        Integer minPrice,
        Integer maxPrice,
        List<PathInfoResponse> pathInfos,
        Long chatRoomId,
        Boolean isHost,
        Boolean isJoined,
        LocalDateTime createdAt
) {
    public static FundingDetailResponse from(
            Funding funding,
            List<PathInfoResponse> pathInfos,
            Integer currentParticipants,
            Long chatRoomId,
            Boolean isHost,
            Boolean isJoined
    ) {

        Integer minPrice = (int) (Math.ceil(
                (double) funding.getTotalPrice() / funding.getMaxParticipants() / 100
        ) * 100);

        Integer maxPrice = (int) (Math.ceil(
                (double) funding.getTotalPrice() / funding.getMinParticipants() / 100
        ) * 100);

        return new FundingDetailResponse(
                funding.getFundingId(),
                funding.getTitle(),
                funding.getContent(),
                funding.getMember().getMemberId(),
                funding.getMember().getNickname(),
                funding.getDepartureDate(),
                funding.getStatus(),
                funding.getBusType(),
                currentParticipants,
                funding.getMinParticipants(),
                funding.getMaxParticipants(),
                funding.getTripType(),
                funding.getTotalPrice(),
                minPrice,
                maxPrice,
                pathInfos,
                chatRoomId,
                isHost,
                isJoined,
                funding.getCreatedAt()
        );
    }
}
