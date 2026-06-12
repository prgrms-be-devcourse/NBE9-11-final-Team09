package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;

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
        Integer totalPrice,
        Integer minPrice,
        Integer maxPrice
) {
    public static FundingListResponse from(
            Funding funding,
            Pathinfo pathinfo,
            Integer currentParticipants
    ) {

        Integer minPrice = (int) (Math.ceil(
                (double) funding.getTotalPrice() / funding.getMaxParticipants() / 100
        ) * 100);

        Integer maxPrice = (int) (Math.ceil(
                (double) funding.getTotalPrice() / funding.getMinParticipants() / 100
        ) * 100);

        return new FundingListResponse(
                funding.getFundingId(),
                funding.getTitle(),
                funding.getMember().getNickname(),
                pathinfo.getDepartureAddress(),
                pathinfo.getArrivalAddress(),
                pathinfo.getDepartureTime(),
                funding.getStatus(),
                currentParticipants,
                funding.getMinParticipants(),
                funding.getMaxParticipants(),
                funding.getTotalPrice(),
                minPrice,
                maxPrice
        );
    }
}
