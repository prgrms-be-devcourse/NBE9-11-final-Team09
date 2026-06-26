package com.back.team9.moyeota.domain.participation.repository;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    // 특정 회원의 중복 참여 확인
    boolean existsByFunding_FundingIdAndMember_MemberId(Long fundingId, Long memberId);

    // (참여 ID와 회원 ID로) 본인 참여 내역 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"outboundSeat", "outboundSeat.pathinfo", "returnSeat"})
    Optional<Participation> findByParticipationIdAndMember_MemberId(Long participationId, Long memberId);

    // 내 참여 내역 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"funding", "outboundSeat", "outboundSeat.pathinfo", "returnSeat"})
    List<Participation> findByMember_MemberIdOrderByCreatedAtDesc(Long memberId);

    //특정 펀딩의 전체 참여자 목록 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"member", "outboundSeat", "returnSeat"})
    List<Participation> findByFunding_FundingId(Long fundingId);

    // 특정 상태의 참여자 수 조회
    long countByFunding_FundingIdAndStatus(Long fundingId, ParticipationStatus status);

    // 취소되지 않은 참여자 수 조회
    long countByFunding_FundingIdAndPaymentStatusIn(
            Long fundingId,
            List<ParticipationPaymentStatus> paymentStatuses
    );

    @Query("""
        SELECT p FROM Participation p
        JOIN FETCH p.outboundSeat os
        JOIN FETCH os.pathinfo pi
        LEFT JOIN FETCH p.returnSeat rs
        WHERE pi.departureTime > :now
          AND pi.departureTime <= :deadline
          AND p.paymentStatus = :paymentStatus
          AND p.status = :status
""")
    List<Participation> findNoShowTargets(
            @Param("now") LocalDateTime now,
            @Param("deadline") LocalDateTime deadline,
            @Param("paymentStatus") ParticipationPaymentStatus paymentStatus,
            @Param("status") ParticipationStatus status
    );

    boolean existsByFunding_FundingIdAndMember_MemberIdAndStatus(
            Long fundingId,
            Long memberId,
            ParticipationStatus status
    );

    @Query("""
        select distinct p.member.memberId
        from Participation p
        where p.funding.fundingId = :fundingId
          and p.status = :status
        """)
    List<Long> findMemberIdsByFundingIdAndStatus(
            @Param("fundingId") Long fundingId,
            @Param("status") ParticipationStatus status
    );

    @Query("""
        select p.funding.fundingId as fundingId,
               count(p) as count
        from Participation p
        where p.funding.fundingId in :fundingIds
          and p.status = :status
        group by p.funding.fundingId
        """)
    List<FundingParticipationCount> countByFundingIdsAndStatus(List<Long> fundingIds, ParticipationStatus status);

    interface FundingParticipationCount {
        Long getFundingId();
        Long getCount();
    }

    List<Participation> findByFunding_FundingIdAndStatus(Long fundingId, ParticipationStatus status);

    @Query("SELECT p FROM Participation p JOIN FETCH p.member JOIN FETCH p.funding WHERE p.participationId = :id")
    Optional<Participation> findWithMemberAndFundingById(@Param("id") Long id);

}
