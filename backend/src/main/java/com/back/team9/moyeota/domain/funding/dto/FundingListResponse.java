package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        BigDecimal totalPrice,
        BigDecimal currentPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
    private static final BigDecimal PRICE_UNIT = BigDecimal.valueOf(100);

    public static FundingListResponse from(
            Funding funding,
            Pathinfo pathinfo,
            Integer currentParticipants
    ) {
        BigDecimal minPrice = calculateRoundedPrice(
                funding.getTotalPrice(),
                funding.getMaxParticipants()
        );

        BigDecimal maxPrice = calculateRoundedPrice(
                funding.getTotalPrice(),
                funding.getMinParticipants()
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
                funding.getTotalPrice(),
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

        return calculateRoundedPrice(
                funding.getTotalPrice(),
                currentParticipants
        );
    }

    private static BigDecimal calculateRoundedPrice(
            BigDecimal totalPrice,
            Integer participants
    ) {
        return totalPrice
                .divide(BigDecimal.valueOf(participants), 0, RoundingMode.CEILING)
                .divide(PRICE_UNIT, 0, RoundingMode.CEILING)
                .multiply(PRICE_UNIT);
    }
}