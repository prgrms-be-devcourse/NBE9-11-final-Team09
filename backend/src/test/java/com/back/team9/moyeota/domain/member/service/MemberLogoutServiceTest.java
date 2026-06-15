package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.jwt.TokenType;
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
        when(jwtTokenProvider.validateToken("access-token"))
                .thenReturn(true);
        when(jwtTokenProvider.getTokenType("access-token"))
                .thenReturn(TokenType.ACCESS);
        when(jwtTokenProvider.getJti("access-token"))
                .thenReturn("access-jti");
        when(jwtTokenProvider.getRemainingExpiration("access-token"))
                .thenReturn(3000L);

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
        when(jwtTokenProvider.validateToken("invalid-token"))
                .thenReturn(false);

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
        when(jwtTokenProvider.validateToken("refresh-token"))
                .thenReturn(true);
        when(jwtTokenProvider.getTokenType("refresh-token"))
                .thenReturn(TokenType.REFRESH);

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