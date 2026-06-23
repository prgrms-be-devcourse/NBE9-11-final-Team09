package com.back.team9.moyeota.domain.payment.entity;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

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

    public void updateStatus(PaymentStatus status){
        this.status = status;
    }

    public void confirm(PaymentType paymentType, String tossPaymentKey) {
        this.paymentType = paymentType;
        this.tossPaymentKey = tossPaymentKey;
        this.status = PaymentStatus.PAID;
    }
    public void startRefund() {
        this.status = PaymentStatus.REFUND_PENDING;
    }
    public void completeRefund() {
        this.status = PaymentStatus.REFUNDED;
    }
    public void expire() {
        this.status = PaymentStatus.FAILED;
    }

}
