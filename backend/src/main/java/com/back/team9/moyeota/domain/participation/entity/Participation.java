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

    // 최종 확정 금액 (출발 -7일 자정에 스케줄러가 계산 후 업데이트)
    // 참여 신청 시점에는 아직 인원이 확정되지 않아 0으로 저장
    @Column(nullable = false)
    private Integer finalAmount;

    // 참여 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    // 가는편 좌석 (필수 - 모든 펀딩은 최소 가는편은 있어야 함)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_seat_id", nullable = false)
    private Seat outboundSeat;

    // 오는편 좌석( 편도 펀딩인 경우 NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_seat_id")
    private Seat returnSeat;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(0)
                .status(ParticipationStatus.ACTIVE)
                .outboundSeat(outboundSeat)
                .returnSeat(returnSeat)
                .createdAt(LocalDateTime.now())
                .build();
    }

    //참여 취소 시 호출되는 비즈니스 메서드
    //환불 가능 여부(출발 -10일 기준) 판단은 Service 계층에서 먼저 검증 후 호출해야 함
    // 이 메서드는 "취소 가능"이 확정된 이후의 상태 변경만 담당
    public void cancel() {
        this.status = ParticipationStatus.CANCELED;
        this.paymentStatus = ParticipationPaymentStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }
}
