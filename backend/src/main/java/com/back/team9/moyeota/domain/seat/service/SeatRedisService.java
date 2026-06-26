package com.back.team9.moyeota.domain.seat.service;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatRedisService {
    private final StringRedisTemplate redisTemplate;

    private static final String SEAT_KEY_PREFIX = "seat:";
    private static final Duration HOLD_DURATION = Duration.ofSeconds(300); // HOLD 5분 유지

    // Lua 스크립트를 상수로 정의하여 재사용 (매 호출마다 새 객체 생성 방지)
    private static final org.springframework.data.redis.core.script.RedisScript<Long> RELEASE_SCRIPT =
            org.springframework.data.redis.core.script.RedisScript.of(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) " +
                            "else " +
                            "return 0 " +
                            "end",
                    Long.class
            );

    //Redis Key 생성, 예: seatId=3 → seat:3
    private String generateKey(Long seatId) {
        return SEAT_KEY_PREFIX + seatId;
    }

    // 좌석 5분 선점 (SET NX EX 사용)
    public void holdSeat(Long seatId, Long memberId) {
        String key = generateKey(seatId);
        String value = String.valueOf(memberId);

        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, HOLD_DURATION); // 키가 없을 때만 저장
            if (!Boolean.TRUE.equals(success)) {
                throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 장애 시 예외 반환 (폴백 없음, MVP 기준)
            log.error("Redis 장애 발생 - 좌석 선점 실패. seatId={}, memberId={}", seatId, memberId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 좌석 선점 해제 (결제 완료 또는 참여 취소 시)
    public boolean releaseSeat(Long seatId, Long memberId) {
        String key = generateKey(seatId);

        try {
            // RELEASE_SCRIPT 상수 재사용 (매 호출마다 새 객체 생성 방지)
            Long result = redisTemplate.execute(
                    RELEASE_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(memberId)
            );

            return result != null && result == 1L;

        } catch (Exception e) {
            log.warn("Redis 장애 발생 - 좌석 해제 실패. seatId={}, memberId={}", seatId, memberId, e);
            return false;
        }
    }

    // 좌석 선점 유효성 확인 (특정 멤버가 해당 좌석을 HOLD 중인지 확인)
    public boolean isHeldBy(Long seatId, Long memberId) {
        String key = generateKey(seatId);

        try {
            String value = redisTemplate.opsForValue().get(key);
            return String.valueOf(memberId).equals(value);

        } catch (Exception e) {
            log.warn("Redis 장애 발생 - 좌석 선점 유효성 확인 실패. seatId={}, memberId={}", seatId, memberId, e);
            return false;
        }
    }

    // 여러 좌석의 선점 유저 ID를 한 번에 조회 (N+1 최적화)
    public Map<Long, Long> getHoldMemberIds(List<Long> seatIds) {
        // 조회할 좌석이 없으면 빈 Map 반환
        if (seatIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // seatId 목록 → Redis Key 목록 변환
        List<String> keys = seatIds.stream()
                .map(this::generateKey)
                .toList();

        try {
            // Redis MGET 수행
            List<String> values =
                    redisTemplate.opsForValue().multiGet(keys);

            if (values == null) {
                return Collections.emptyMap();
            }

            Map<Long, Long> holdMap = new HashMap<>();
            int size = Math.min(seatIds.size(), values.size());
            for (int i = 0; i < size; i++) {
                String val = values.get(i);
                if (val != null) {
                    try {
                        holdMap.put(seatIds.get(i), Long.parseLong(val));
                    } catch (NumberFormatException e) {
                        log.warn("Redis HOLD 값 변환 실패. seatId={}, value={}", seatIds.get(i), val);
                    }
                }
            }
            return holdMap;

        } catch (Exception e) {
            log.error("Redis 장애 발생 - 다중 홀딩 유저 조회 실패. seatIds={}", seatIds, e);
            return Collections.emptyMap();
        }
    }
}


