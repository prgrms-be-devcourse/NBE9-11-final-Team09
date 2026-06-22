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

    // (참여 ID와 회원 ID로) 본인 참여 내역 조회
    //N+1 방지를 위해 연관 데이터를 함께 조회
    @EntityGraph(attributePaths = {"outboundSeat", "outboundSeat.pathinfo", "returnSeat"})
    Optional<Participation> findByParticipationIdAndMember_MemberId(Long participationId, Long memberId);

    //특정 펀딩의 전체 참여자 목록 조회
    //N+1 방지를 위해 연관 데이터를 함께 조회
    @EntityGraph(attributePaths = {"member", "outboundSeat", "returnSeat"})
    List<Participation> findByFunding_FundingId(Long fundingId);

    // 특정 상태의 참여자 수 조회
    long countByFunding_FundingIdAndStatus(Long fundingId, ParticipationStatus status);

    // 취소되지 않은 참여자 수 조회
    long countByFunding_FundingIdAndPaymentStatusIn(
            Long fundingId,
            List<ParticipationPaymentStatus> paymentStatuses
    );
    // 출발 24시간 전 기준으로 payment_status = ACTIVE인 참여자 조회( NO_SHOW 처리 대상)
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
    // 펀딩 목록의 각 펀딩 참여자 수 조회
    @Query("""
        select p.funding.fundingId as fundingId,
               count(p) as count
        from Participation p
        where p.funding.fundingId in :fundingIds
          and p.status = :status
        group by p.funding.fundingId
        """)
    List<FundingParticipationCount> countByFundingIdsAndStatus(List<Long> fundingIds, ParticipationStatus status);

    // 참가자 수 인터페이스
    interface FundingParticipationCount {
        Long getFundingId();
        Long getCount();
    }
}
