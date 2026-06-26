package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREPARE_LOCK_PREFIX = "payment:prepare:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    public void lockPrepare(Long participationId) {
        String key = PREPARE_LOCK_PREFIX + participationId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.PAYMENT_IN_PROGRESS);
        }
    }

    public void unlockPrepare(Long participationId) {
        redisTemplate.delete(PREPARE_LOCK_PREFIX + participationId);
    }
}
