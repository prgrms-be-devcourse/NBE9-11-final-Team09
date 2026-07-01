package com.back.team9.moyeota.domain.funding.event;

import com.back.team9.moyeota.domain.funding.entity.Funding;

public record FundingCreatedEvent(
        Funding funding,
        String hostOutboundSeatNumber,
        String hostReturnSeatNumber
) {
}
