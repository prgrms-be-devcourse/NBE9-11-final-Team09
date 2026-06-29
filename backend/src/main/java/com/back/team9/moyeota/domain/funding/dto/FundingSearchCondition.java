package com.back.team9.moyeota.domain.funding.dto;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;

import java.time.LocalDate;
import java.util.List;

public record FundingSearchCondition(
        List<FundingStatus> statuses,
        LocalDate departureDate,
        Region departureRegion,
        Region arrivalRegion,
        TripType tripType
) {
    public List<FundingStatus> effectiveStatuses() {
        if (statuses == null || statuses.isEmpty()) {
            return List.of(
                    FundingStatus.RECRUITING, //디폴트는 참가 가능한 펀딩만 보여줌
                    FundingStatus.CONFIRMED
            );
        }

        return statuses;
    }
}
