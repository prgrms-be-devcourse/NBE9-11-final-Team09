package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.repository.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFundingService {

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
}