package com.back.team9.moyeota.domain.admin.repository;

import com.back.team9.moyeota.domain.admin.dto.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.dto.AdminFundingStatistics;
import org.springframework.data.repository.query.Param;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminFundingQueryRepository extends JpaRepository<Funding, Long> {

    long countByStatus(FundingStatus status);

    @Query(
            value = """
                    select new com.back.team9.moyeota.domain.admin.dto.AdminFundingListResponse(
                        f.fundingId,
                        f.member.memberId,
                        f.member.email,
                        f.title,
                        f.content,
                        f.departureDate,
                        f.busType,
                        f.status,
                        f.minParticipants,
                        f.maxParticipants,
                        count(p),
                        f.createdAt
                    )
                    from Funding f
                    left join Participation p
                        on p.funding = f
                        and p.status <> :canceledStatus
                    group by
                        f.fundingId,
                        f.member.memberId,
                        f.member.email,
                        f.title,
                        f.content,
                        f.departureDate,
                        f.busType,
                        f.status,
                        f.minParticipants,
                        f.maxParticipants,
                        f.createdAt
                    """,
            countQuery = """
                    select count(f)
                    from Funding f
                    """
    )
    Page<AdminFundingListResponse> findAdminFundings(
            ParticipationStatus canceledStatus,
            Pageable pageable
    );

    @Query("""
        SELECT new com.back.team9.moyeota.domain.admin.dto.AdminFundingStatistics(
            COUNT(CASE
                WHEN f.status = :activeStatus THEN 1
                ELSE NULL
            END),
            COUNT(CASE
                WHEN f.status = :completedStatus THEN 1
                ELSE NULL
            END),
            COUNT(CASE
                WHEN f.status = :cancelledStatus THEN 1
                ELSE NULL
            END)
        )
        FROM Funding f
        """)
    AdminFundingStatistics findStatistics(
            @Param("activeStatus") FundingStatus activeStatus,
            @Param("completedStatus") FundingStatus completedStatus,
            @Param("cancelledStatus") FundingStatus cancelledStatus
    );
}