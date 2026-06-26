package com.back.team9.moyeota.domain.seat.repository;

import com.back.team9.moyeota.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByPathinfo_PathinfoId(Long pathinfoId);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            DELETE FROM Seat s
            WHERE s.pathinfo.pathinfoId = :pathinfoId
            """)
    void deleteByPathinfo_PathinfoId(
            @Param("pathinfoId") Long pathinfoId
    );

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
