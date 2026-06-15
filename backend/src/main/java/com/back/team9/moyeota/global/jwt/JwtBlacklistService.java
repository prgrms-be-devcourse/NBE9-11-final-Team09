package com.back.team9.moyeota.global.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";
    private static final String LOGOUT_VALUE = "logout";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, long remainingExpiration) {
        if (remainingExpiration <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                KEY_PREFIX + jti,
                LOGOUT_VALUE,
                Duration.ofMillis(remainingExpiration)
        );
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(KEY_PREFIX + jti)
        );
    }
}