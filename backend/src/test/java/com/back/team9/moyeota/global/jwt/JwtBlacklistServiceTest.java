package com.back.team9.moyeota.global.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 블랙리스트 서비스 테스트")
class JwtBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Access Token의 jti를 남은 유효시간 동안 블랙리스트에 등록한다")
    void blacklistStoresJtiWithRemainingExpiration() {
        // Given
        JwtBlacklistService service =
                new JwtBlacklistService(redisTemplate);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        service.blacklist("access-jti", 3000);

        // Then
        verify(valueOperations).set(
                "jwt:blacklist:access-jti",
                "logout",
                Duration.ofMillis(3000)
        );
    }

    @Test
    @DisplayName("남은 유효시간이 없으면 블랙리스트에 등록하지 않는다")
    void blacklistWithNoRemainingExpirationDoesNotStoreJti() {
        // Given
        JwtBlacklistService service =
                new JwtBlacklistService(redisTemplate);

        // When
        service.blacklist("access-jti", 0);

        // Then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Redis에 jti가 존재하면 블랙리스트 토큰으로 판단한다")
    void isBlacklistedReturnsTrueWhenJtiExists() {
        // Given
        JwtBlacklistService service =
                new JwtBlacklistService(redisTemplate);

        when(redisTemplate.hasKey("jwt:blacklist:access-jti"))
                .thenReturn(true);

        // When / Then
        org.assertj.core.api.Assertions.assertThat(
                service.isBlacklisted("access-jti")
        ).isTrue();
    }

    @Test
    @DisplayName("Redis에 jti가 없으면 블랙리스트 토큰이 아니다")
    void isBlacklistedReturnsFalseWhenJtiDoesNotExist() {
        // Given
        JwtBlacklistService service =
                new JwtBlacklistService(redisTemplate);

        when(redisTemplate.hasKey("jwt:blacklist:access-jti"))
                .thenReturn(false);

        // When / Then
        org.assertj.core.api.Assertions.assertThat(
                service.isBlacklisted("access-jti")
        ).isFalse();
    }
}