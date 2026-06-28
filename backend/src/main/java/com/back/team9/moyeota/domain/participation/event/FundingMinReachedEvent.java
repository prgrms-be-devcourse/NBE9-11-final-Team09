package com.back.team9.moyeota.domain.participation.event;

public record FundingMinReachedEvent(
        Long fundingId,
        Long hostMemberId
) {
}
