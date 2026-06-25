package com.back.team9.moyeota.domain.member.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 인증 요청 제한 Redis 저장소 테스트")
class EmailVerificationRateLimitRedisRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailVerificationRateLimitRedisRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EmailVerificationRateLimitRedisRepository(redisTemplate);
    }

    @Test
    @DisplayName("요청 제한 락 획득에 성공하면 true를 반환한다")
    void tryLockRequestReturnsTrueWhenLockAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                "member:email-verification-request-lock:moyeota@example.com",
                "1",
                Duration.ofMinutes(1)
        )).thenReturn(true);

        boolean result = repository.tryLockRequest(" MOYEOTA@example.com ");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이미 요청 제한 락이 존재하면 false를 반환한다")
    void tryLockRequestReturnsFalseWhenLockAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                "member:email-verification-request-lock:moyeota@example.com",
                "1",
                Duration.ofMinutes(1)
        )).thenReturn(false);

        boolean result = repository.tryLockRequest(" MOYEOTA@example.com ");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("요청 제한 락을 1분 TTL로 원자적으로 저장한다")
    void tryLockRequestStoresKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        repository.tryLockRequest(" MOYEOTA@example.com ");

        verify(valueOperations).setIfAbsent(
                eq("member:email-verification-request-lock:moyeota@example.com"),
                eq("1"),
                eq(Duration.ofMinutes(1))
        );
    }

    @Test
    @DisplayName("요청 제한 락을 삭제한다")
    void deleteRequestLockDeletesKey() {
        repository.deleteRequestLock(" MOYEOTA@example.com ");

        verify(redisTemplate).delete(
                "member:email-verification-request-lock:moyeota@example.com"
        );
    }
}