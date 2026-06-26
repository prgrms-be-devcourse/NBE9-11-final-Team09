package com.back.team9.moyeota.domain.seat.entity;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.global.entity.BaseEntity;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id")
    private Participation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_member_id")
    private Member hostMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pathinfo_id", nullable = false)
    private Pathinfo pathinfo;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    // ==================== 생성자 ====================
    @Builder
    private Seat(Pathinfo pathinfo, String seatNumber) {
        this.pathinfo = pathinfo;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE; // 생성 시 기본값은 항상 AVAILABLE
    }

    // ==================== 비즈니스 메서드 ====================
    // 결제 완료 시 좌석 예약 확정
    public void book(Participation participation) {
        if (this.status == SeatStatus.BOOKED) { // 이미 예약된 좌석 방어 코드
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }
        this.participation = participation;
        this.status = SeatStatus.BOOKED;
    }

    public void bookByHost(Member hostMember) {
        if (this.status == SeatStatus.BOOKED) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        this.hostMember = hostMember;
        this.status = SeatStatus.BOOKED;
    }

    // 참여 취소 시 좌석 해제
    public void release() {
        this.participation = null;
        this.hostMember = null;
        this.status = SeatStatus.AVAILABLE;
    }
}
