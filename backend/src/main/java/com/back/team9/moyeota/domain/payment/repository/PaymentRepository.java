package com.back.team9.moyeota.domain.payment.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);
    List<Payment> findByParticipation_ParticipationId(Long participationId);
    Optional<Payment> findByParticipation_ParticipationIdAndStatus(Long participationId, PaymentStatus status);
    List<Payment> findAllByParticipation_ParticipationIdAndStatus(Long participationId, PaymentStatus status);
    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.participation.funding.fundingId = :fundingId AND p.status = :status")
    BigDecimal sumAmountByFundingIdAndStatus(@Param("fundingId") Long fundingId, @Param("status") PaymentStatus status);

}
