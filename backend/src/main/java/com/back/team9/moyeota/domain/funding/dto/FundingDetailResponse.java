package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.policy.FundingPricePolicy;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.pathinfo.dto.PathinfoResponse;



import java.math.BigDecimal;
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
        BigDecimal totalPrice,
        BigDecimal finalPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<PathinfoResponse> pathinfos,
        Long chatRoomId,
        Boolean isHost,
        Boolean isJoined,
        Long myParticipationId,
        ParticipationPaymentStatus myPaymentStatus,
        Boolean isCanceled,
        LocalDateTime createdAt
) {
    public static FundingDetailResponse from(
            Funding funding,
            List<PathinfoResponse> pathinfos,
            Integer currentParticipants,
            Long chatRoomId,
            Boolean isHost,
            Boolean isJoined,
            Long myParticipationId,
            ParticipationPaymentStatus myPaymentStatus,
            Boolean isCanceled
    ) {

        BigDecimal minPrice = FundingPricePolicy.calculateRoundedPrice(
                funding.getTotalPrice(),
                funding.getMaxParticipants() + 1
        );

        BigDecimal maxPrice = FundingPricePolicy.calculateRoundedPrice(
                funding.getTotalPrice(),
                funding.getMinParticipants() + 1
        );

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
                funding.getFinalPrice(),
                minPrice,
                maxPrice,
                pathinfos,
                chatRoomId,
                isHost,
                isJoined,
                myParticipationId,
                myPaymentStatus,
                isCanceled,
                funding.getCreatedAt()
        );
    }
}
