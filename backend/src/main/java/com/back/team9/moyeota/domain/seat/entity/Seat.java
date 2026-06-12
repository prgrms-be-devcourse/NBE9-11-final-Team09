package com.back.team9.moyeota.domain.seat.entity;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "seat") //DB의 seat 테이블과 연결됨
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY) // 좌석을 예약한 참여자
    @JoinColumn(name = "participation_id") // participation_id FK(외래키), 예약 전은 null 가능
    private Participation participation;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 좌석이 하나의 노선에 속함
    @JoinColumn(name = "pathinfo_id", nullable = false) // pathinfo_id FK, 필수 연관관계
    private PathInfo pathInfo; // pathinfo → pathInfo (camelCase 컨벤션)

    @Column(nullable = false) // 버스 좌석 번호 (예: 1A, 2B)
    private String seatNumber;

    @Enumerated(EnumType.STRING) // Enum 이름 그대로 DB에 저장
    @Column(nullable = false) // DB에는 AVAILABLE, BOOKED만 저장
    private SeatStatus status;

    @Column(nullable = false, updatable = false) // 생성 시각, 수정 불가
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt; // 마지막 수정 시각

    // ==================== 생성자 ====================
    @Builder // 펀딩 생성 시 좌석을 생성하기 위한 빌더
    private Seat(PathInfo pathInfo, String seatNumber) {
        this.pathInfo = pathInfo;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE; // 생성 시 기본값은 항상 AVAILABLE
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 비즈니스 메서드 ====================
    // 결제 완료 시 좌석 예약 확정
    public void book(Participation participation) {
        if (this.status == SeatStatus.BOOKED) { // 이미 예약된 좌석 방어 코드
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }
        this.participation = participation;
        this.status = SeatStatus.BOOKED;
        this.updatedAt = LocalDateTime.now();
    }

    // 참여 취소 시 좌석 해제
    public void release() {
        this.participation = null;
        this.status = SeatStatus.AVAILABLE;
        this.updatedAt = LocalDateTime.now();
    }
}
