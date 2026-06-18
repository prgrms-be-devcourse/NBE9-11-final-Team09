package com.back.team9.moyeota.domain.admin.repository;

import com.back.team9.moyeota.domain.settlement.entity.Settlement;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSettlementQueryRepository
        extends JpaRepository<Settlement, Long> {

    long countByStatus(SettlementStatus status);

    @Override
    @EntityGraph(attributePaths = {"member", "funding"})
    Page<Settlement> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "funding"})
    Optional<Settlement> findBySettlementId(Long settlementId);
}