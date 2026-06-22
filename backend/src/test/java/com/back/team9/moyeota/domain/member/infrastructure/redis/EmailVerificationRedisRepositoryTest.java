package com.back.team9.moyeota.domain.member.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 인증 Redis 저장소 테스트")
class EmailVerificationRedisRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private EmailVerificationRedisRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new EmailVerificationRedisRepository(
                redisTemplate,
                objectMapper
        );
    }

    @Test
    @DisplayName("인증정보를 TTL 30분으로 저장한다")
    void saveStoresVerificationDataWithTtl() {
        EmailVerificationData data =
                new EmailVerificationData("encoded-code");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        repository.save("moyeota@example.com", data);

        verify(valueOperations).set(
                eq("member:email-verification:moyeota@example.com"),
                anyString(),
                eq(Duration.ofMinutes(30))
        );

        verify(valueOperations).set(
                "member:email-verification-attempts:moyeota@example.com",
                "0",
                Duration.ofMinutes(30)
        );
    }

    @Test
    @DisplayName("이메일로 인증정보를 조회한다")
    void findByEmailReturnsVerificationData() throws Exception {
        EmailVerificationData data =
                new EmailVerificationData("encoded-code");
        String value = objectMapper.writeValueAsString(data);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(
                "member:email-verification:moyeota@example.com"
        )).thenReturn(value);

        Optional<EmailVerificationData> result =
                repository.findByEmail("moyeota@example.com");

        assertThat(result).contains(data);
    }

    @Test
    @DisplayName("인증정보가 없으면 빈 값을 반환한다")
    void findByEmailReturnsEmptyWhenDataDoesNotExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(
                "member:email-verification:moyeota@example.com"
        )).thenReturn(null);

        Optional<EmailVerificationData> result =
                repository.findByEmail("moyeota@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("인증 실패 횟수를 원자적으로 증가시킨다")
    void incrementFailedAttemptsIncrementsRedisCounter() {
        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.increment(
                "member:email-verification-attempts:moyeota@example.com"
        )).thenReturn(3L);

        long result = repository.incrementFailedAttempts(
                "moyeota@example.com"
        );

        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("인증정보와 실패 횟수를 함께 삭제한다")
    void deleteByEmailDeletesVerificationAndAttemptKeys() {
        repository.deleteByEmail(" MOYEOTA@EXAMPLE.COM ");

        verify(redisTemplate).delete(java.util.List.of(
                "member:email-verification:moyeota@example.com",
                "member:email-verification-attempts:moyeota@example.com"
        ));
    }
}