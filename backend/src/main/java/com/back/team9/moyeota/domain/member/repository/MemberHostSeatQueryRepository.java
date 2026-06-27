package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.member.repository.projection.MemberHostSeatSummary;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberHostSeatQueryRepository
        extends JpaRepository<Seat, Long> {

    @Query("""
        select
            p.funding.fundingId as fundingId,
            s.seatNumber as seatNumber
        from Seat s
        join s.pathinfo p
        where s.hostMember.memberId = :memberId
          and p.funding.fundingId in :fundingIds
        order by p.pathinfoId asc, s.seatNumber asc
        """)
    List<MemberHostSeatSummary> findHostSeatsByFundingIds(
            Long memberId,
            Collection<Long> fundingIds
    );
}