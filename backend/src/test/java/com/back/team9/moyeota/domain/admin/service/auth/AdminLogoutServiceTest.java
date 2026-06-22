package com.back.team9.moyeota.domain.admin.service.auth;

import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtAuthenticationInfo;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.PrincipalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 로그아웃 서비스 테스트")
class AdminLogoutServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @InjectMocks
    private AdminLogoutService adminLogoutService;

    @Test
    @DisplayName("유효한 관리자 Access Token을 블랙리스트에 등록한다")
    void logoutWithValidAdminTokenAddsTokenToBlacklist() {
        // Given
        String accessToken = "admin-access-token";

        JwtAuthenticationInfo authenticationInfo =
                new JwtAuthenticationInfo(
                        1L,
                        PrincipalType.ADMIN,
                        "SUPER_ADMIN",
                        "admin-access-jti"
                );

        when(jwtTokenProvider.findAuthenticationInfo(accessToken))
                .thenReturn(Optional.of(authenticationInfo));
        when(jwtTokenProvider.getRemainingExpiration(accessToken))
                .thenReturn(3000L);

        // When
        adminLogoutService.logout(accessToken);

        // Then
        verify(jwtBlacklistService).blacklist(
                "admin-access-jti",
                3000L
        );
    }

    @Test
    @DisplayName("회원 Access Token으로 관리자 로그아웃을 요청하면 예외가 발생한다")
    void logoutWithMemberTokenThrowsException() {
        // Given
        String accessToken = "member-access-token";

        JwtAuthenticationInfo authenticationInfo =
                new JwtAuthenticationInfo(
                        1L,
                        PrincipalType.MEMBER,
                        "MEMBER",
                        "member-access-jti"
                );

        when(jwtTokenProvider.findAuthenticationInfo(accessToken))
                .thenReturn(Optional.of(authenticationInfo));

        // When / Then
        assertThatThrownBy(() ->
                adminLogoutService.logout(accessToken)
        ).isInstanceOf(BusinessException.class);

        verify(jwtTokenProvider, never())
                .getRemainingExpiration(accessToken);
        verifyNoInteractions(jwtBlacklistService);
    }

    @Test
    @DisplayName("유효하지 않은 Access Token이면 예외가 발생한다")
    void logoutWithInvalidTokenThrowsException() {
        // Given
        String accessToken = "invalid-token";

        when(jwtTokenProvider.findAuthenticationInfo(accessToken))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                adminLogoutService.logout(accessToken)
        ).isInstanceOf(BusinessException.class);

        verify(jwtTokenProvider, never())
                .getRemainingExpiration(accessToken);
        verifyNoInteractions(jwtBlacklistService);
    }

    @Test
    @DisplayName("남은 유효시간이 없으면 블랙리스트에 등록하지 않는다")
    void logoutWithNoRemainingExpirationDoesNotAddToBlacklist() {
        // Given
        String accessToken = "admin-access-token";

        JwtAuthenticationInfo authenticationInfo =
                new JwtAuthenticationInfo(
                        1L,
                        PrincipalType.ADMIN,
                        "SUPER_ADMIN",
                        "admin-access-jti"
                );

        when(jwtTokenProvider.findAuthenticationInfo(accessToken))
                .thenReturn(Optional.of(authenticationInfo));
        when(jwtTokenProvider.getRemainingExpiration(accessToken))
                .thenReturn(0L);

        // When
        adminLogoutService.logout(accessToken);

        // Then
        verify(jwtBlacklistService).blacklist(
                "admin-access-jti",
                0L
        );
    }
}