package com.back.team9.moyeota.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long fundingId;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer platformFee;

    @Column(nullable = false)
    private Integer hostPaybackAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(nullable = false)
    private LocalDateTime paybackPaidAt;

    @Column(nullable = false)
    private Boolean paybackHold;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}