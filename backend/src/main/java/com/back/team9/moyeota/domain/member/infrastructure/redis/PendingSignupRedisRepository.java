package com.back.team9.moyeota.domain.member.infrastructure.redis;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PendingSignupRedisRepository {

    private static final String KEY_PREFIX = "member:signup:";
    private static final Duration SIGNUP_TTL =
            Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(PendingSignupData signupData) {
        try {
            String value = objectMapper.writeValueAsString(signupData);

            redisTemplate.opsForValue().set(
                    generateKey(signupData.email()),
                    value,
                    SIGNUP_TTL
            );
        } catch (JacksonException | DataAccessException exception) {
            log.error("회원가입 대기 정보 Redis 저장 실패", exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<PendingSignupData> findByEmail(String email) {
        try {
            String value = redisTemplate.opsForValue().get(
                    generateKey(email)
            );

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(
                    value,
                    PendingSignupData.class
            ));
        } catch (JacksonException | DataAccessException exception) {
            log.error("회원가입 대기 정보 Redis 조회 실패", exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteByEmail(String email) {
        try {
            redisTemplate.delete(generateKey(email));
        } catch (DataAccessException exception) {
            log.error("회원가입 대기 정보 Redis 삭제 실패", exception);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String generateKey(String email) {
        return KEY_PREFIX
                + email.trim().toLowerCase(Locale.ROOT);
    }
}