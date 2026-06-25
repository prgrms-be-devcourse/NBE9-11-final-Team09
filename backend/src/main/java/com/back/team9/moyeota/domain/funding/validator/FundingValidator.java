package com.back.team9.moyeota.domain.funding.validator;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;

public final class FundingValidator {

    private FundingValidator() {
    }

    public static void validateFundingRequest(
            Integer minParticipants,
            BusType busType
    ) {

        if (minParticipants > busType.getCapacity()) {
            throw new BusinessException(
                    ErrorCode.FUNDING_MIN_INVALID
            );
        }
    }

    public static void validateHost(
            Funding funding,
            Long memberId
    ) {
        if (!funding.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FUNDING_FORBIDDEN);
        }
    }

    // 확정 이전의 펀딩만 수정 및 삭제 가능
    public static void validateRecruitingStatus(Funding funding) {
        if (funding.getStatus() != FundingStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.FUNDING_RESTRICTED_UPDATE_OR_CANCEL);
        }
    }
}
