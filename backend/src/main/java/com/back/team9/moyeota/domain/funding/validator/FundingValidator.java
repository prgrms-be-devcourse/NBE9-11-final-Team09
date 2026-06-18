package com.back.team9.moyeota.domain.funding.validator;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class FundingValidator {

    public void validateFundingRequest(
            Integer minParticipants,
            BusType busType
    ) {

        if (minParticipants > busType.getCapacity()) {
            throw new BusinessException(
                    ErrorCode.FUNDING_MIN_INVALID
            );
        }
    }

    public void validateHost(
            Funding funding,
            Long memberId
    ) {
        if (!funding.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FUNDING_FORBIDDEN);
        }
    }

    public void validateUpdatable(Funding funding) {
        if (funding.getStatus() != FundingStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.FUNDING_RESTRICTED_UPDATE);
        }
    }
}
