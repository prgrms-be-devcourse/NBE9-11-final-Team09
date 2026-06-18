package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.AdminFundingCancelRequest;
import com.back.team9.moyeota.domain.admin.dto.AdminFundingCancelResponse;
import com.back.team9.moyeota.domain.admin.dto.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.repository.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminFundingService {

    private static final Set<FundingStatus> CANCELLABLE_STATUSES = Set.of(
            FundingStatus.RECRUITING,
            FundingStatus.CONFIRMED
    );

    private final AdminFundingQueryRepository fundingRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminFundingListResponse> getFundings(Pageable pageable) {
        return PageResponse.from(
                fundingRepository.findAdminFundings(
                        ParticipationStatus.CANCELED,
                        pageable
                )
        );
    }

    @Transactional
    public AdminFundingCancelResponse cancelFunding(
            Long fundingId,
            AdminFundingCancelRequest request
    ) {
        validateCancelReason(request.reason());

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FUNDING_NOT_FOUND
                ));

        validateCancellableStatus(funding);

        funding.cancel();

        return AdminFundingCancelResponse.from(funding);
    }

    private void validateCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateCancellableStatus(Funding funding) {
        if (funding.getStatus() == FundingStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.ADMIN_FUNDING_ALREADY_CANCELLED
            );
        }

        if (!CANCELLABLE_STATUSES.contains(funding.getStatus())) {
            throw new BusinessException(
                    ErrorCode.ADMIN_FUNDING_CANCEL_NOT_ALLOWED
            );
        }
    }
}