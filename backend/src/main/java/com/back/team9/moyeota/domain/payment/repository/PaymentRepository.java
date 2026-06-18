package com.back.team9.moyeota.domain.payment.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);
    Optional<Payment> findByParticipation_ParticipationId(Long participationId);
    Optional<Payment> findByParticipation_ParticipationIdAndStatus(Long participationId, PaymentStatus status);


}
