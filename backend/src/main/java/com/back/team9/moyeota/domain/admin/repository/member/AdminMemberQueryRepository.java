package com.back.team9.moyeota.domain.admin.repository.member;

import com.back.team9.moyeota.domain.admin.dto.statistics.AdminMemberStatistics;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.back.team9.moyeota.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminMemberQueryRepository extends JpaRepository<Member, Long> {

    long countByStatus(MemberStatus status);

    @Query("select count(p) from Participation p where p.member.memberId = :memberId")
    long countParticipationsByMemberId(Long memberId);

    @Query("select count(f) from Funding f where f.member.memberId = :memberId")
    long countFundingsByMemberId(Long memberId);

    @Query("""
            select count(p)
            from Payment p
            where p.participation.member.memberId = :memberId
            """)
    long countPaymentsByMemberId(Long memberId);

    @Query("""
        SELECT new com.back.team9.moyeota.domain.admin.dto.statistics.AdminMemberStatistics(
            COUNT(m),
            COUNT(CASE
                WHEN m.status = :activeStatus THEN 1
                ELSE NULL
            END),
            COUNT(CASE
                WHEN m.status = :withdrawnStatus THEN 1
                ELSE NULL
            END)
        )
        FROM Member m
        """)
    AdminMemberStatistics findStatistics(
            @Param("activeStatus") MemberStatus activeStatus,
            @Param("withdrawnStatus") MemberStatus withdrawnStatus
    );
}
