package com.back.team9.moyeota.domain.admin.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminPaymentQueryRepository extends JpaRepository<Payment, Long> {

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = :status
            """)
    Long sumAmountByStatus(PaymentStatus status);

    long countByParticipation_Funding_FundingIdAndStatus(
            Long fundingId,
            PaymentStatus status
    );

    long countByParticipation_Funding_FundingIdAndPaymentTypeAndStatus(
            Long fundingId,
            PaymentType paymentType,
            PaymentStatus status
    );

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.participation.funding.fundingId = :fundingId
              and p.status = :status
            """)
    Long sumAmountByFundingIdAndStatus(
            Long fundingId,
            PaymentStatus status
    );
}