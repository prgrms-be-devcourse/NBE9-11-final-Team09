package com.back.team9.moyeota.domain.funding.validator;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class FundingValidator {

    public void validateFundingRequest(
            Integer minParticipants,
            BusType busType,
            Integer totalPrice
    ) {

        if (minParticipants > busType.getCapacity()) {
            throw new BusinessException(
                    ErrorCode.FUNDING_MIN_INVALID
            );
        }
    }
}
