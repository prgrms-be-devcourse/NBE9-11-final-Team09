package com.back.team9.moyeota.domain.participation.entity;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"funding_id", "member_id"})
})
public class Participation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    // 참여한 펀딩 (N:1 - 하나의 펀딩에 여러 참여자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private Funding funding;

    // 참여 회원 (N:1 - 한 유저는 여러 펀딩에 참여 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationPaymentStatus paymentStatus;

    // 참여 신청 시점에는 아직 인원이 확정되지 않아 BigDecimal.ZERO로 저장
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal finalAmount;

    // 참여 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    // 가는편 좌석 (필수)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_seat_id", nullable = false)
    private Seat outboundSeat;

    // 오는편 좌석( 편도 펀딩인 경우 NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_seat_id")
    private Seat returnSeat;

    //참여 신청 시 호출되는 생성 메서드
    public static Participation create(
            Funding funding,
            Member member,
            Seat outboundSeat,
            Seat returnSeat
    ) {
        return Participation.builder()
                .funding(funding)
                .member(member)
                .paymentStatus(ParticipationPaymentStatus.PENDING)
                .finalAmount(BigDecimal.ZERO)
                .status(ParticipationStatus.ACTIVE)
                .outboundSeat(outboundSeat)
                .returnSeat(returnSeat)
                .build();
    }

    // 결제 완료 후 좌석 확정 시 호출
    public void confirmPayment() {
        this.paymentStatus = ParticipationPaymentStatus.ACTIVE;
    }

    //참여 취소 시 호출되는 비즈니스 메서드
    public void cancel() {
        this.status = ParticipationStatus.CANCELED;
        this.paymentStatus = ParticipationPaymentStatus.CANCELED;
    }

    // 잔액 결제 완료 시 호출
    public void completePayment() {
        this.paymentStatus = ParticipationPaymentStatus.COMPLETED;
    }

    public void updateFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    // 잔액 미납으로 NO_SHOW 처리 시 호출
    public void markAsNoShow() {
        this.status = ParticipationStatus.CANCELED;
        this.paymentStatus = ParticipationPaymentStatus.NO_SHOW;
    }
}
