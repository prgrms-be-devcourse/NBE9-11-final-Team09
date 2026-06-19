package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 로그아웃 서비스 테스트")
class MemberLogoutServiceTest {

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @InjectMocks
    private MemberLogoutService memberLogoutService;

    @Test
    @DisplayName("유효한 Access Token을 블랙리스트에 등록한다")
    void logoutWithValidAccessTokenBlacklistsToken() {
        // Given
        when(jwtTokenResolver.findToken("Bearer access-token"))
                .thenReturn(Optional.of("access-token"));
        when(jwtTokenProvider.findAccessTokenInfo("access-token"))
                .thenReturn(Optional.of(new JwtAccessTokenInfo(
                        1L,
                        "access-jti",
                        3000L
                )));

        // When
        memberLogoutService.logout("Bearer access-token");

        // Then
        verify(jwtBlacklistService).blacklist("access-jti", 3000L);
    }

    @Test
    @DisplayName("Authorization 헤더 형식이 잘못되면 로그아웃에 실패한다")
    void logoutWithInvalidAuthorizationThrowsException() {
        // Given
        when(jwtTokenResolver.findToken("invalid-header"))
                .thenReturn(Optional.empty());

        // When / Then
        assertTokenInvalidException(
                () -> memberLogoutService.logout("invalid-header")
        );

        verifyNoInteractions(jwtTokenProvider, jwtBlacklistService);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 로그아웃에 실패한다")
    void logoutWithInvalidTokenThrowsException() {
        // Given
        when(jwtTokenResolver.findToken("Bearer invalid-token"))
                .thenReturn(Optional.of("invalid-token"));
        when(jwtTokenProvider.findAccessTokenInfo("invalid-token"))
                .thenReturn(Optional.empty());

        // When / Then
        assertTokenInvalidException(
                () -> memberLogoutService.logout(
                        "Bearer invalid-token"
                )
        );

        verifyNoInteractions(jwtBlacklistService);
    }

    @Test
    @DisplayName("Refresh Token으로 로그아웃을 요청하면 실패한다")
    void logoutWithRefreshTokenThrowsException() {
        // Given
        when(jwtTokenResolver.findToken("Bearer refresh-token"))
                .thenReturn(Optional.of("refresh-token"));
        when(jwtTokenProvider.findAccessTokenInfo("refresh-token"))
                .thenReturn(Optional.empty());

        // When / Then
        assertTokenInvalidException(
                () -> memberLogoutService.logout(
                        "Bearer refresh-token"
                )
        );

        verifyNoInteractions(jwtBlacklistService);
    }

    private void assertTokenInvalidException(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TOKEN_INVALID)
                );
    }
}
