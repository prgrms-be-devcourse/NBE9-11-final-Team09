package com.back.team9.moyeota.domain.seat.repository;

import com.back.team9.moyeota.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
