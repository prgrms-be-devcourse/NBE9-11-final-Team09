package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.MemberWithdrawRequest;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 탈퇴 서비스 테스트")
class MemberWithdrawServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberLogoutService memberLogoutService;

    @InjectMocks
    private MemberWithdrawService memberWithdrawService;

    @Test
    @DisplayName("비밀번호가 일치하면 회원 상태를 탈퇴로 변경하고 토큰을 무효화한다")
    void withdrawWithValidPasswordChangesStatusAndLogsOut() {
        // Given
        Member member = createMember(MemberStatus.ACTIVE, "encoded-password");
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("Password123!");
        String authorization = "Bearer access-token";

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Password123!", "encoded-password"))
                .thenReturn(true);

        // When
        memberWithdrawService.withdraw(1L, request, authorization);

        // Then
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getUpdatedAt()).isNotNull();

        verify(memberLogoutService).logout(authorization);
    }

    @Test
    @DisplayName("회원 ID가 없으면 회원 탈퇴에 실패한다")
    void withdrawWithNullMemberIdThrowsException() {
        // Given
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("Password123!");

        // When / Then
        assertBusinessException(
                () -> memberWithdrawService.withdraw(
                        null,
                        request,
                        "Bearer access-token"
                ),
                ErrorCode.USER_NOT_FOUND
        );

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(memberLogoutService);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 회원 탈퇴에 실패한다")
    void withdrawWithUnknownMemberThrowsException() {
        // Given
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("Password123!");

        when(memberRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When / Then
        assertBusinessException(
                () -> memberWithdrawService.withdraw(
                        1L,
                        request,
                        "Bearer access-token"
                ),
                ErrorCode.USER_NOT_FOUND
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(memberLogoutService);
    }

    @Test
    @DisplayName("이미 탈퇴한 회원이면 회원 탈퇴에 실패한다")
    void withdrawWithWithdrawnMemberThrowsException() {
        // Given
        Member member = createMember(
                MemberStatus.WITHDRAWN,
                "encoded-password"
        );
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("Password123!");

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // When / Then
        assertBusinessException(
                () -> memberWithdrawService.withdraw(
                        1L,
                        request,
                        "Bearer access-token"
                ),
                ErrorCode.USER_ALREADY_WITHDRAWN
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(memberLogoutService);
    }

    @Test
    @DisplayName("정지된 회원이면 회원 탈퇴에 실패한다")
    void withdrawWithSuspendedMemberThrowsException() {
        // Given
        Member member = createMember(
                MemberStatus.SUSPENDED,
                "encoded-password"
        );
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("Password123!");

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // When / Then
        assertBusinessException(
                () -> memberWithdrawService.withdraw(
                        1L,
                        request,
                        "Bearer access-token"
                ),
                ErrorCode.USER_SUSPENDED
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(memberLogoutService);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 회원 탈퇴에 실패한다")
    void withdrawWithInvalidPasswordThrowsException() {
        // Given
        Member member = createMember(MemberStatus.ACTIVE, "encoded-password");
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("WrongPassword123!");
        String authorization = "Bearer access-token";

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches(
                "WrongPassword123!",
                "encoded-password"
        )).thenReturn(false);

        // When / Then
        assertBusinessException(
                () -> memberWithdrawService.withdraw(
                        1L,
                        request,
                        authorization
                ),
                ErrorCode.INVALID_LOGIN_CREDENTIALS
        );

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberLogoutService, never()).logout(anyString());
    }

    @Test
    @DisplayName("비밀번호가 없는 회원이면 회원 탈퇴에 실패한다")
    void withdrawWithNullPasswordThrowsException() {
        // Given
        Member member = createMember(MemberStatus.ACTIVE, null);
        MemberWithdrawRequest request =
                new MemberWithdrawRequest("Password123!");

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // When / Then
        assertBusinessException(
                () -> memberWithdrawService.withdraw(
                        1L,
                        request,
                        "Bearer access-token"
                ),
                ErrorCode.INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(memberLogoutService);
    }

    private Member createMember(
            MemberStatus status,
            String password
    ) {
        return Member.builder()
                .memberId(1L)
                .email("member@example.com")
                .password(password)
                .name("홍길동")
                .nickname("모여타요")
                .phoneNumber("010-1234-5678")
                .status(status)
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }
}