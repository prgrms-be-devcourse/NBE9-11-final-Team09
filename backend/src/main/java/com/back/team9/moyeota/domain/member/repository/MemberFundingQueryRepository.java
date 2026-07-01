package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.repository.projection.MemberFundingSummary;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberFundingQueryRepository
        extends JpaRepository<Funding, Long> {

    @Query(
            value = """
                    select
                        f.fundingId as fundingId,
                        f.title as fundingTitle,
                        f.departureDate as departureDate,
                        count(p) as currentParticipants,
                        f.maxParticipants as maxParticipants,
                        f.status as status,
                        f.createdAt as createdAt
                    from Funding f
                    left join Participation p
                        on p.funding = f
                        and p.status <> :canceledStatus
                    where f.member.memberId = :memberId
                    group by
                        f.fundingId,
                        f.title,
                        f.departureDate,
                        f.maxParticipants,
                        f.status,
                        f.createdAt
                    """,
            countQuery = """
                    select count(f)
                    from Funding f
                    where f.member.memberId = :memberId
                    """
    )
    Page<MemberFundingSummary> findMyFundings(
            Long memberId,
            ParticipationStatus canceledStatus,
            Pageable pageable
    );
}