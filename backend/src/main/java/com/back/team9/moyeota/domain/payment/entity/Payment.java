package com.back.team9.moyeota.domain.payment.entity;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private PaymentType paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = true, unique = true)
    private String tossPaymentKey;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String failReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void updateStatus(PaymentStatus status){
        this.status = status;
    }

    public void confirm(PaymentType paymentType, String tossPaymentKey) {
        this.paymentType = paymentType;
        this.tossPaymentKey = tossPaymentKey;
        this.status = PaymentStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }
    public void startRefund() {
        this.status = PaymentStatus.REFUND_PENDING;
        this.updatedAt = LocalDateTime.now();
    }
    public void completeRefund() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }
    public void expire() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

}
