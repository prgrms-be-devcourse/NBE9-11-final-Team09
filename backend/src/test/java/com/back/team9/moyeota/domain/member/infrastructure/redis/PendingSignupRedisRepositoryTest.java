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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 대기 Redis 저장소 테스트")
class PendingSignupRedisRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private PendingSignupRedisRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        repository = new PendingSignupRedisRepository(
                redisTemplate,
                objectMapper
        );
    }

    @Test
    @DisplayName("가입 대기 정보를 TTL 30분으로 저장한다")
    void saveStoresPendingSignupWithTtl() {
        PendingSignupData signupData = createSignupData();

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        repository.save(signupData);

        verify(valueOperations).set(
                eq("member:signup:moyeota@example.com"),
                anyString(),
                eq(Duration.ofMinutes(30))
        );
    }

    @Test
    @DisplayName("이메일로 가입 대기 정보를 조회한다")
    void findByEmailReturnsPendingSignup() throws Exception {
        PendingSignupData signupData = createSignupData();
        String value = objectMapper.writeValueAsString(signupData);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(
                "member:signup:moyeota@example.com"
        )).thenReturn(value);

        Optional<PendingSignupData> result =
                repository.findByEmail("moyeota@example.com");

        assertThat(result).contains(signupData);
    }

    @Test
    @DisplayName("가입 대기 정보가 없으면 빈 값을 반환한다")
    void findByEmailReturnsEmptyWhenDataDoesNotExist() {
        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(valueOperations.get(
                "member:signup:moyeota@example.com"
        )).thenReturn(null);

        Optional<PendingSignupData> result =
                repository.findByEmail("moyeota@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이메일을 정규화하여 가입 대기 정보를 삭제한다")
    void deleteByEmailDeletesNormalizedKey() {
        repository.deleteByEmail(" MOYEOTA@EXAMPLE.COM ");

        verify(redisTemplate).delete(
                "member:signup:moyeota@example.com"
        );
    }

    private PendingSignupData createSignupData() {
        return new PendingSignupData(
                "moyeota@example.com",
                "encoded-password",
                "홍길동",
                "모여타요",
                "010-1234-5678",
                "verification-code-hash"
        );
    }
}