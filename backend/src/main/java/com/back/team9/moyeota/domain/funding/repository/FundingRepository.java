package com.back.team9.moyeota.domain.funding.repository;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingRepository extends JpaRepository<Funding, Long>, FundingRepositoryCustom {
}
