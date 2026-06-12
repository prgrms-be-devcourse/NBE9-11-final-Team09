package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.entity.PendingMemberSignup;
import com.back.team9.moyeota.domain.member.repository.PendingMemberSignupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 대기 정보 서비스 테스트")
class PendingMemberSignupServiceTest {

    @Mock
    private PendingMemberSignupRepository pendingSignupRepository;

    @InjectMocks
    private PendingMemberSignupService pendingMemberSignupService;

    @Test
    @DisplayName("신규 회원가입 요청이면 가입 대기 정보를 저장한다")
    void saveOrUpdateWithNewEmailSavesPendingSignup() {
        // Given
        MemberSignupRequest request = createSignupRequest();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        // When
        pendingMemberSignupService.saveOrUpdate(
                request,
                "encoded-password",
                "encoded-code",
                expiresAt
        );

        // Then
        verify(pendingSignupRepository)
                .save(org.mockito.ArgumentMatchers.any(
                        PendingMemberSignup.class
                ));
    }

    @Test
    @DisplayName("동일한 이메일로 재요청하면 기존 가입 대기 정보를 갱신한다")
    void saveOrUpdateWithExistingEmailUpdatesPendingSignup() {
        // Given
        MemberSignupRequest request = createSignupRequest();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        PendingMemberSignup existingSignup = PendingMemberSignup.create(
                request.email(),
                "old-password",
                "기존이름",
                "기존닉네임",
                "010-0000-0000",
                "old-code",
                LocalDateTime.now().plusMinutes(10)
        );

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(existingSignup));

        // When
        pendingMemberSignupService.saveOrUpdate(
                request,
                "new-password",
                "new-code",
                expiresAt
        );

        // Then
        verify(pendingSignupRepository).save(existingSignup);

        assertThat(existingSignup.getEncodedPassword())
                .isEqualTo("new-password");
        assertThat(existingSignup.getName()).isEqualTo(request.name());
        assertThat(existingSignup.getNickname())
                .isEqualTo(request.nickname());
        assertThat(existingSignup.getPhoneNumber())
                .isEqualTo(request.phoneNumber());
        assertThat(existingSignup.getVerificationCodeHash())
                .isEqualTo("new-code");
        assertThat(existingSignup.getExpiresAt()).isEqualTo(expiresAt);
    }

    private MemberSignupRequest createSignupRequest() {
        return new MemberSignupRequest(
                "moyeota@example.com",
                "Password123!",
                "홍길동",
                "모여타요",
                "010-1234-5678"
        );
    }
}