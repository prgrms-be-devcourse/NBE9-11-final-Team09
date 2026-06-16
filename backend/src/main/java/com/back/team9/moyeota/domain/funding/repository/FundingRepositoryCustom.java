package com.back.team9.moyeota.domain.funding.repository;

import com.back.team9.moyeota.domain.funding.dto.FundingSearchCondition;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FundingRepositoryCustom {
    Page<Funding> findPageByCondition(
            FundingSearchCondition condition,
            Pageable pageable
    );
}
