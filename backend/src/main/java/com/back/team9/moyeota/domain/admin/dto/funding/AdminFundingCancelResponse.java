package com.back.team9.moyeota.domain.admin.dto.funding;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;

public record AdminFundingCancelResponse(
        Long fundingId,
        FundingStatus status
) {
    public static AdminFundingCancelResponse from(Funding funding) {
        return new AdminFundingCancelResponse(
                funding.getFundingId(),
                funding.getStatus()
        );
    }
}
