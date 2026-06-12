package com.back.team9.moyeota.global.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 토큰 제공자 테스트")
class JwtTokenProviderTest {

    private static final long ACCESS_EXPIRATION = 3_600_000;
    private static final long REFRESH_EXPIRATION = 1_209_600_000;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901"
                        .getBytes(StandardCharsets.UTF_8)
        );

        jwtTokenProvider = new JwtTokenProvider(
                secret,
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION
        );
    }

    @Test
    @DisplayName("Access Token과 Refresh Token을 생성한다")
    void createTokensReturnsAccessAndRefreshTokens() {
        // When
        JwtTokenResponse response = jwtTokenProvider.createTokens(1L);

        // Then
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.accessTokenExpiresIn()).isEqualTo(3600);
        assertThat(response.refreshTokenExpiresIn()).isEqualTo(1209600);
    }

    @Test
    @DisplayName("생성된 토큰에서 회원 ID와 토큰 타입을 추출한다")
    void createdTokenContainsMemberIdAndTokenType() {
        // Given
        JwtTokenResponse response = jwtTokenProvider.createTokens(1L);

        // Then
        assertThat(jwtTokenProvider.getMemberId(response.accessToken()))
                .isEqualTo(1L);
        assertThat(jwtTokenProvider.getTokenType(response.accessToken()))
                .isEqualTo(TokenType.ACCESS);
        assertThat(jwtTokenProvider.getTokenType(response.refreshToken()))
                .isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token은 서로 다른 jti를 가진다")
    void createdTokensHaveDifferentJti() {
        // Given
        JwtTokenResponse response = jwtTokenProvider.createTokens(1L);

        // When
        String accessJti = jwtTokenProvider.getJti(response.accessToken());
        String refreshJti = jwtTokenProvider.getJti(response.refreshToken());

        // Then
        assertThat(accessJti).isNotBlank();
        assertThat(refreshJti).isNotBlank();
        assertThat(accessJti).isNotEqualTo(refreshJti);
    }

    @Test
    @DisplayName("정상 토큰은 유효성 검증에 성공한다")
    void validateTokenWithValidTokenReturnsTrue() {
        // Given
        JwtTokenResponse response = jwtTokenProvider.createTokens(1L);

        // Then
        assertThat(jwtTokenProvider.validateToken(response.accessToken()))
                .isTrue();
        assertThat(jwtTokenProvider.getRemainingExpiration(
                response.accessToken()
        )).isPositive();
    }

    @Test
    @DisplayName("위조된 토큰은 유효성 검증에 실패한다")
    void validateTokenWithTamperedTokenReturnsFalse() {
        // Given
        JwtTokenResponse response = jwtTokenProvider.createTokens(1L);
        String tamperedToken = response.accessToken() + "tampered";

        // Then
        assertThat(jwtTokenProvider.validateToken(tamperedToken))
                .isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효성 검증에 실패한다")
    void validateTokenWithExpiredTokenReturnsFalse() {
        // Given
        String expiredToken = jwtTokenProvider.createToken(
                1L,
                TokenType.ACCESS,
                -1000
        );

        // Then
        assertThat(jwtTokenProvider.validateToken(expiredToken))
                .isFalse();
    }

    @Test
    @DisplayName("만료된 토큰의 남은 유효 시간은 0을 반환한다")
    void getRemainingExpirationWithExpiredTokenReturnsZero() {
        // Given
        String expiredToken = jwtTokenProvider.createToken(
                1L,
                TokenType.ACCESS,
                -1000
        );

        // When
        long remainingExpiration =
                jwtTokenProvider.getRemainingExpiration(expiredToken);

        // Then
        assertThat(remainingExpiration).isZero();
    }
}