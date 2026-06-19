package com.back.team9.moyeota.domain.seat.repository;

import com.back.team9.moyeota.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 특정 노선의 전체 좌석 조회 (좌석 배치도 조회용)
    // findByPathinfo_PathinfoId → pathinfo 필드의 pathinfoId로 조회
    List<Seat> findByPathinfo_PathinfoId(Long pathinfoId);

    void deleteByPathinfo_PathinfoId(Long pathinfoId);

    // 좌석, 노선, 펀딩 함께 조회
    @Query("""
            SELECT s
            FROM Seat s
            JOIN FETCH s.pathinfo p
            JOIN FETCH p.funding
            WHERE s.seatId = :seatId
            """)
    Optional<Seat> findByIdWithPathinfoAndFunding(
            @Param("seatId") Long seatId
    );
}
