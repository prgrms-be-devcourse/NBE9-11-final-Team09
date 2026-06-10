package com.back.team9.moyeota.domain.participation.repository;

import com.back.team9.moyeota.domain.participation.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
}
