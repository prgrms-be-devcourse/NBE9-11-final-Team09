package com.back.team9.moyeota.domain.funding.repository;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long> {
    // TODO: 페이징 정책에 따른 Batchsize 방식으로 전환 가능
    @Query("""
            select f
            from Funding f
            join fetch f.member
            """)
    List<Funding> findAllWithMember();

    @Query("select f.member.memberId from Funding f where f.fundingId = :fundingId")
    Long findHostIdByFundingId(@Param("fundingId") Long fundingId);
}
