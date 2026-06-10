package com.back.team9.moyeota.domain.participation.entity;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"funding_id", "member_id"})
})
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private Funding funding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationPaymentStatus paymentStatus;

    @Column(nullable = false)
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_seat_id", nullable = false)
    private Seat outboundSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_seat_id")
    private Seat returnSeat;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
