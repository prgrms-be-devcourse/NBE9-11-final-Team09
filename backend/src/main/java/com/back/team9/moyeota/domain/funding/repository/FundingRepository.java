package com.back.team9.moyeota.domain.funding.repository;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FundingRepository extends JpaRepository<Funding, Long>, FundingRepositoryCustom {

    List<Funding> findByStatusAndDepartureDateLessThanEqual(
            FundingStatus status,
            LocalDate departureDate
    );

    List<Funding> findByStatusAndDepartureDateBefore(
            FundingStatus status,
            LocalDate departureDate
    );
  
    @Query("select f.member.memberId from Funding f where f.fundingId = :fundingId")
    Long findHostIdByFundingId(@Param("fundingId") Long fundingId);

    @Query("""
            select f from Funding f
            where f.status = 'COMPLETED'
              and f.departureDate < :today
              and not exists (select 1 from Settlement s where s.funding = f)
            """)
    List<Funding> findCompletedWithoutSettlement(@Param("today") LocalDate today);
    List<Funding> findByStatusAndDepartureDate(FundingStatus status, LocalDate departureDate);
}
