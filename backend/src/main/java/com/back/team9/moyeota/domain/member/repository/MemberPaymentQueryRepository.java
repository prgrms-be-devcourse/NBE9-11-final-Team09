package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPaymentQueryRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {
            "participation",
            "participation.funding"
    })
    Page<Payment> findByParticipation_Member_MemberId(
            Long memberId,
            Pageable pageable
    );
}