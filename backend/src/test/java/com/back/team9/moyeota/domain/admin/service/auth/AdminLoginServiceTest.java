package com.back.team9.moyeota.domain.admin.service.auth;

import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginRequest;
import com.back.team9.moyeota.domain.admin.dto.auth.AdminLoginResponse;
import com.back.team9.moyeota.domain.admin.entity.Admin;
import com.back.team9.moyeota.domain.admin.entity.AdminRole;
import com.back.team9.moyeota.domain.admin.entity.AdminStatus;
import com.back.team9.moyeota.domain.admin.repository.auth.AdminRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.dto.JwtAccessTokenResponse;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 로그인 서비스 테스트")
class AdminLoginServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AdminLoginService adminLoginService;

    @Test
    @DisplayName("올바른 관리자 정보로 로그인하면 Access Token을 반환한다")
    void loginWithValidCredentialsReturnsAccessToken() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(
                "admin",
                "Password123!"
        );
        Admin admin = createAdmin(AdminStatus.ACTIVE, "encoded-password");

        when(adminRepository.findByLoginId("admin"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(
                "Password123!",
                "encoded-password"
        )).thenReturn(true);
        when(jwtTokenProvider.createAdminAccessToken(
                1L,
                AdminRole.SUPER_ADMIN.name()
        )).thenReturn(new JwtAccessTokenResponse(
                "admin-access-token",
                3600
        ));

        // When
        AdminLoginResponse response = adminLoginService.login(request);

        // Then
        assertThat(response.accessToken()).isEqualTo("admin-access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(3600);
        assertThat(response.admin().adminId()).isEqualTo(1L);
        assertThat(response.admin().loginId()).isEqualTo("admin");
        assertThat(response.admin().role()).isEqualTo("SUPER_ADMIN");

        verify(jwtTokenProvider).createAdminAccessToken(
                1L,
                "SUPER_ADMIN"
        );
    }

    @Test
    @DisplayName("존재하지 않는 관리자 아이디로 로그인하면 예외가 발생한다")
    void loginWithUnknownLoginIdThrowsException() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(
                "unknown",
                "Password123!"
        );

        when(adminRepository.findByLoginId("unknown"))
                .thenReturn(Optional.empty());

        // When / Then
        assertBusinessException(
                () -> adminLoginService.login(request),
                ErrorCode.ADMIN_INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다")
    void loginWithInvalidPasswordThrowsException() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(
                "admin",
                "WrongPassword!"
        );
        Admin admin = createAdmin(AdminStatus.ACTIVE, "encoded-password");

        when(adminRepository.findByLoginId("admin"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(
                "WrongPassword!",
                "encoded-password"
        )).thenReturn(false);

        // When / Then
        assertBusinessException(
                () -> adminLoginService.login(request),
                ErrorCode.ADMIN_INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("비밀번호가 없는 관리자 계정은 로그인할 수 없다")
    void loginWithNullPasswordThrowsException() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(
                "admin",
                "Password123!"
        );
        Admin admin = createAdmin(AdminStatus.ACTIVE, null);

        when(adminRepository.findByLoginId("admin"))
                .thenReturn(Optional.of(admin));

        // When / Then
        assertBusinessException(
                () -> adminLoginService.login(request),
                ErrorCode.ADMIN_INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("정지된 관리자 계정으로 로그인하면 예외가 발생한다")
    void loginWithSuspendedAdminThrowsException() {
        // Given
        AdminLoginRequest request = new AdminLoginRequest(
                "admin",
                "Password123!"
        );
        Admin admin = createAdmin(
                AdminStatus.SUSPENDED,
                "encoded-password"
        );

        when(adminRepository.findByLoginId("admin"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(
                "Password123!",
                "encoded-password"
        )).thenReturn(true);

        // When / Then
        assertBusinessException(
                () -> adminLoginService.login(request),
                ErrorCode.ADMIN_SUSPENDED
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    private Admin createAdmin(
            AdminStatus status,
            String password
    ) {
        return Admin.builder()
                .adminId(1L)
                .loginId("admin")
                .password(password)
                .role(AdminRole.SUPER_ADMIN)
                .status(status)
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
