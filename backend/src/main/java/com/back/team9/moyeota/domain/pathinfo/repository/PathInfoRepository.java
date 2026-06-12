package com.back.team9.moyeota.domain.pathinfo.repository;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PathInfoRepository extends JpaRepository<PathInfo, Long> {
    List<PathInfo> findByFunding_FundingId(Long fundingId);
    List<PathInfo> findByFunding_FundingIdInAndDirection(
            List<Long> fundingIds,
            Direction direction
    );
}
