package com.back.team9.moyeota.domain.payment.repository;

import com.back.team9.moyeota.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
