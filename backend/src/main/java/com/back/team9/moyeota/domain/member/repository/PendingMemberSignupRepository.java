package com.back.team9.moyeota.domain.member.repository;

import com.back.team9.moyeota.domain.member.entity.PendingMemberSignup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingMemberSignupRepository
        extends JpaRepository<PendingMemberSignup, Long> {

    Optional<PendingMemberSignup> findByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}