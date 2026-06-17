package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberParticipationQueryRepository
        extends JpaRepository<Participation, Long> {

    @EntityGraph(attributePaths = "funding")
    Page<Participation> findByMember_MemberId(
            Long memberId,
            Pageable pageable
    );
}