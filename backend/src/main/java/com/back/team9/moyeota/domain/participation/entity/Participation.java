package com.back.team9.moyeota.domain.participation.entity;

import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    @Column(nullable = false)
    private Long fundingId;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    @Column(nullable = false)
    private String outboundSeatId;

    private String returnSeatId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
