package com.back.team9.moyeota.domain.participation.repository;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    // 특정 회원의 중복 참여 확인
    boolean existsByFunding_FundingIdAndMember_MemberId(Long fundingId, Long memberId);

    // (참여 ID와 회원 ID로) 본인 참여 내역 조회
    Optional<Participation> findByParticipationIdAndMember_MemberId(Long participationId, Long memberId);

    //특정 펀딩의 전체 참여자 목록 조회
    //N+1 방지를 위해 연관 데이터를 함께 조회
    @EntityGraph(attributePaths = {"member", "outboundSeat", "returnSeat"})
    List<Participation> findByFunding_FundingId(Long fundingId);

    // 특정 상태의 참여자 수 조회
    long countByFunding_FundingIdAndStatus(Long fundingId, ParticipationStatus status);
}
