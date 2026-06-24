package com.back.team9.moyeota.domain.member.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("이메일 인증 요청 제한 Redis 저장소 테스트")
class EmailVerificationRateLimitRedisRepositoryTest {

    private final StringRedisTemplate redisTemplate =
            mock(StringRedisTemplate.class);

    private final ValueOperations<String, String> valueOperations =
            mock(ValueOperations.class);

    private final EmailVerificationRateLimitRedisRepository repository =
            new EmailVerificationRateLimitRedisRepository(redisTemplate);

    @Test
    @DisplayName("요청 제한 키가 존재하면 제한 상태로 판단한다")
    void isRequestLockedReturnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey(
                "member:email-verification-request-lock:moyeota@example.com"
        )).thenReturn(true);

        boolean result = repository.isRequestLocked(" MOYEOTA@example.com ");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("요청 제한 키가 없으면 제한 상태가 아니라고 판단한다")
    void isRequestLockedReturnsFalseWhenKeyDoesNotExist() {
        when(redisTemplate.hasKey(
                "member:email-verification-request-lock:moyeota@example.com"
        )).thenReturn(false);

        boolean result = repository.isRequestLocked(" MOYEOTA@example.com ");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("요청 제한 키를 1분 TTL로 저장한다")
    void lockRequestStoresKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        repository.lockRequest(" MOYEOTA@example.com ");

        verify(valueOperations).set(
                eq("member:email-verification-request-lock:moyeota@example.com"),
                eq("1"),
                eq(Duration.ofMinutes(1))
        );
    }

    @Test
    @DisplayName("요청 제한 키를 삭제한다")
    void deleteRequestLockDeletesKey() {
        repository.deleteRequestLock(" MOYEOTA@example.com ");

        verify(redisTemplate).delete(
                "member:email-verification-request-lock:moyeota@example.com"
        );
    }
}