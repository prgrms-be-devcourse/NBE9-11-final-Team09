package com.back.team9.moyeota.domain.settlement.entity;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement extends BaseEntity {

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

    public void approve(LocalDateTime paybackPaidAt) {
        this.status = SettlementStatus.APPROVED;
        this.paybackPaidAt = paybackPaidAt;
    }

    public void reject() {
        this.status = SettlementStatus.REJECTED;
    }
}
