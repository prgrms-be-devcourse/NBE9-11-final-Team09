package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.entity.PendingMemberSignup;
import com.back.team9.moyeota.domain.member.repository.PendingMemberSignupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PendingMemberSignupService {

    private final PendingMemberSignupRepository pendingSignupRepository;

    @Transactional
    public void saveOrUpdate(
            MemberSignupRequest request,
            String encodedPassword,
            String verificationCodeHash,
            LocalDateTime expiresAt
    ) {
        PendingMemberSignup pendingSignup = pendingSignupRepository
                .findByEmail(request.email())
                .map(existingSignup -> {
                    existingSignup.update(
                            encodedPassword,
                            request.name(),
                            request.nickname(),
                            request.phoneNumber(),
                            verificationCodeHash,
                            expiresAt
                    );
                    return existingSignup;
                })
                .orElseGet(() -> request.toEntity(
                        encodedPassword,
                        verificationCodeHash,
                        expiresAt
                ));

        pendingSignupRepository.save(pendingSignup);
    }
}