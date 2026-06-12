package com.back.team9.moyeota.domain.pathinfo.repository;

import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PathinfoRepository extends JpaRepository<Pathinfo, Long> {
    List<Pathinfo> findByFunding_FundingId(Long fundingId);
    Optional<Pathinfo> findByFunding_FundingIdAndDirection(
            Long fundingId,
            Direction direction
    );
    Optional<Pathinfo> findByFunding_FundingIdAndDirectionAndStatusNot(
            Long fundingId,
            Direction direction,
            PathinfoStatus status
    );

    List<Pathinfo> findByFunding_FundingIdAndStatusNot(
            Long fundingId,
            PathinfoStatus status
    );

    List<Pathinfo> findByFunding_FundingIdInAndDirectionAndStatusNot(
            List<Long> fundingIds,
            Direction direction,
            PathinfoStatus status
    );
}
