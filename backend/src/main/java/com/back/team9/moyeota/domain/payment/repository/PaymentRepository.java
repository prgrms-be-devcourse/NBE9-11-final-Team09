package com.back.team9.moyeota.domain.payment.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);
    // 참여 취소 시 보증금 환불을 위해 participationId로 Payment 조회
    Optional<Payment> findByParticipation_ParticipationId(Long participationId);

}
