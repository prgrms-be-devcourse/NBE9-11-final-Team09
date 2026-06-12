package com.back.team9.moyeota.domain.seat.repository;

import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 특정 노선의 전체 좌석 조회 (좌석 배치도 조회용)
    // findByPathInfo_PathinfoId → pathInfo 필드의 pathinfoId로 조회
    List<Seat> findByPathInfoPathinfoId(Long pathinfoId);

    // 특정 노선의 특정 상태 좌석 조회 (AVAILABLE / BOOKED) (추후 필요 시 사용)
    //List<Seat> findByPathInfoIdAndStatus(Long pathInfoId, SeatStatus status);

}