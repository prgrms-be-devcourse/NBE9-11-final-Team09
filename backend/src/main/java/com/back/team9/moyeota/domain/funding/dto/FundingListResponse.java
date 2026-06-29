package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.policy.FundingPricePolicy;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundingListResponse(
        Long fundingId,
        String title,
        String hostNickname,
        String departureAddress,
        String arrivalAddress,
        LocalDateTime departureTime,
        FundingStatus status,
        Integer currentParticipants,
        Integer minParticipants,
        Integer maxParticipants,
        TripType tripType,
        BigDecimal totalPrice,
        BigDecimal finalPrice,
        BigDecimal currentPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
    public static FundingListResponse from(
            Funding funding,
            Pathinfo pathinfo,
            Integer currentParticipants
    ) {
        BigDecimal minPrice = FundingPricePolicy.calculateRoundedPrice(
                funding.getTotalPrice(),
                funding.getMaxParticipants() + 1
        );

        BigDecimal maxPrice = FundingPricePolicy.calculateRoundedPrice(
                funding.getTotalPrice(),
                funding.getMinParticipants() + 1
        );

        BigDecimal currentPrice = calculateCurrentPrice(
                funding,
                currentParticipants
        );

        return new FundingListResponse(
                funding.getFundingId(),
                funding.getTitle(),
                funding.getMember().getNickname(),
                pathinfo != null ? pathinfo.getDepartureAddress() : null,
                pathinfo != null ? pathinfo.getArrivalAddress() : null,
                pathinfo != null ? pathinfo.getDepartureTime() : null,
                funding.getStatus(),
                currentParticipants,
                funding.getMinParticipants(),
                funding.getMaxParticipants(),
                funding.getTripType(),
                funding.getTotalPrice(),
                funding.getFinalPrice(),
                currentPrice,
                minPrice,
                maxPrice
        );
    }

    private static BigDecimal calculateCurrentPrice(
            Funding funding,
            Integer currentParticipants
    ) {
        if (currentParticipants == null
                || currentParticipants < funding.getMinParticipants()) {
            return null;
        }

        return FundingPricePolicy.calculateRoundedPrice(
                funding.getTotalPrice(),
                currentParticipants + 1
        );
    }
}
