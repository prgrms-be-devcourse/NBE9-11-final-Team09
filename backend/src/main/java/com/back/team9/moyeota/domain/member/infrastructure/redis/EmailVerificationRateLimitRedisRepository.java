package com.back.team9.moyeota.domain.member.infrastructure.redis;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailVerificationRateLimitRedisRepository {

    private static final String REQUEST_LOCK_KEY_PREFIX =
            "member:email-verification-request-lock:";

    private static final Duration REQUEST_COOLDOWN =
            Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public boolean isRequestLocked(String email) {
        try {
            Boolean exists = redisTemplate.hasKey(generateRequestLockKey(email));

            return Boolean.TRUE.equals(exists);
        } catch (DataAccessException exception) {
            log.error("이메일 인증 요청 제한 정보 Redis 조회 실패", exception);

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    public void lockRequest(String email) {
        try {
            redisTemplate.opsForValue().set(
                    generateRequestLockKey(email),
                    "1",
                    REQUEST_COOLDOWN
            );
        } catch (DataAccessException exception) {
            log.error("이메일 인증 요청 제한 정보 Redis 저장 실패", exception);

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    public void deleteRequestLock(String email) {
        try {
            redisTemplate.delete(generateRequestLockKey(email));
        } catch (DataAccessException exception) {
            log.error("이메일 인증 요청 제한 정보 Redis 삭제 실패", exception);

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String generateRequestLockKey(String email) {
        return REQUEST_LOCK_KEY_PREFIX
                + email.trim().toLowerCase(Locale.ROOT);
    }
}