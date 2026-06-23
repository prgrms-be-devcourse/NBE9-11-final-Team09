package com.back.team9.moyeota.domain.pathinfo.repository;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PathinfoRepository extends JpaRepository<Pathinfo, Long> {
    // 특정 펀딩의 모든 노선 조회
    List<Pathinfo> findByFunding_FundingId(Long fundingId);

    // 특정 펀딩의 특정 방향 노선 조회
    Optional<Pathinfo> findByFunding_FundingIdAndDirection(
            Long fundingId,
            Direction direction
    );

    // 특정 펀딩의 특정 방향 노선 중 특정 상태 제외
    Optional<Pathinfo> findByFunding_FundingIdAndDirectionAndStatusNot(
            Long fundingId,
            Direction direction,
            PathinfoStatus status
    );

    // 여러 펀딩의 특정 방향 노선 일괄 조회
    // 취소/실패 펀딩 조회 고려해 모든 상태 조회
    List<Pathinfo> findByFunding_FundingIdInAndDirection(
            List<Long> fundingIds,
            Direction direction
    );

    // 특정 펀딩의 노선 목록 조회
    List<Pathinfo> findByFunding_FundingIdAndStatusNot(
            Long fundingId,
            PathinfoStatus status
    );

    // 유효한 노선들과 연결된 펀딩 조회
    @Query("""
        select p
        from Pathinfo p
        join fetch p.funding f
        where p.status = :status
          and p.departureTime <= :now
          and f.status = :fundingStatus
        """)
    List<Pathinfo> findPathinfosWithFunding(
            PathinfoStatus status,
            LocalDateTime now,
            FundingStatus fundingStatus
    );

    @Query("""
        select p
        from Pathinfo p
        join fetch p.funding f
        join fetch f.member
        where p.status = :status
          and p.direction = :direction
          and p.departureTime > :now
          and p.departureTime <= :reminderDeadline
          and f.status = :fundingStatus
        """)
    List<Pathinfo> findDepartureReminderTargets(
            @Param("status") PathinfoStatus status,
            @Param("direction") Direction direction,
            @Param("now") LocalDateTime now,
            @Param("reminderDeadline") LocalDateTime reminderDeadline,
            @Param("fundingStatus") FundingStatus fundingStatus
    );

    List<Pathinfo> findByFunding_FundingIdInAndStatusNot(
            List<Long> fundingIds,
            PathinfoStatus status
    );
}
