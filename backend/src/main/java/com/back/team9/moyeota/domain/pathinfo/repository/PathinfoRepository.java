package com.back.team9.moyeota.domain.pathinfo.repository;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PathinfoRepository extends JpaRepository<Pathinfo, Long> {
    List<Pathinfo> findByFunding_FundingId(Long fundingId);
    List<Pathinfo> findByFunding_FundingIdInAndDirection(
            List<Long> fundingIds,
            Direction direction
    );
}
