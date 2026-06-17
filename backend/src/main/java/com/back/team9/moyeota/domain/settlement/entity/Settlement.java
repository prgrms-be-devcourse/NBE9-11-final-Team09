package com.back.team9.moyeota.domain.settlement.entity;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false, unique = true)
    private Funding funding;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private BigDecimal platformFee;

    @Column(nullable = false)
    private BigDecimal hostPaybackAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    private LocalDateTime paybackPaidAt;

    @Column(nullable = false)
    private Boolean paybackHold;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void approve(){
        LocalDateTime now = LocalDateTime.now();
        this.status = SettlementStatus.APPROVED;
        this.paybackPaidAt = now;
        this.updatedAt = now;
    }
    public void reject(){
        LocalDateTime now = LocalDateTime.now();
        this.status = SettlementStatus.REJECTED;
        this.updatedAt = now;    }
}
