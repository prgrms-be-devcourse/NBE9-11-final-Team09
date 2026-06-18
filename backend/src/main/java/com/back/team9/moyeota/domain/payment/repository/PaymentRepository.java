package com.back.team9.moyeota.domain.payment.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);
    List<Payment> findByParticipation_ParticipationId(Long participationId);
    Optional<Payment> findByParticipation_ParticipationIdAndStatus(Long participationId, PaymentStatus status);
    List<Payment> findAllByParticipation_ParticipationIdAndStatus(Long participationId, PaymentStatus status);
}
