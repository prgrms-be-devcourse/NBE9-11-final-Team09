package com.back.team9.moyeota.domain.settlement.repository;

import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByFunding_FundingId(Long fundingId);
    boolean existsByFunding_FundingId(Long fundingId);
}
