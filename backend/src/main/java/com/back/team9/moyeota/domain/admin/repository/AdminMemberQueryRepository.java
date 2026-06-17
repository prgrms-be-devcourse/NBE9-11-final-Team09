package com.back.team9.moyeota.domain.admin.repository;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}