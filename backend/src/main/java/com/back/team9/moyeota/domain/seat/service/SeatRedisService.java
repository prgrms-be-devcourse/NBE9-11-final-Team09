package com.back.team9.moyeota.domain.seat.service;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j  // 로그 출력용
@Service // 좌석 HOLD 관련 Redis 작업 전담
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class SeatRedisService {
    private final StringRedisTemplate redisTemplate; // Redis 문자열 저장/조회

    private static final String SEAT_KEY_PREFIX = "seat:"; // Redis Key 접두사
    private static final Duration HOLD_DURATION = Duration.ofSeconds(300); // HOLD 5분 유지

    //Redis Key 생성, 예: seatId=3 → seat:3
    private String generateKey(Long seatId) {
        return SEAT_KEY_PREFIX + seatId;
    }

    // 좌석 5분 선점 (SET NX EX 사용)
    public void holdSeat(Long seatId, Long memberId) {
        String key = generateKey(seatId);
        String value = String.valueOf(memberId);

        try {
            // SET NX EX: 키가 없을 때만 저장 성공 → true 반환
            // 이미 키가 있으면 → false 반환
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, HOLD_DURATION); // 키가 없을 때만 저장

            if (Boolean.FALSE.equals(success)) {
                throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED); // 이미 선점된 좌석
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
    public void releaseSeat(Long seatId, Long memberId) {
        String key = generateKey(seatId);

        try {
            String savedValue = redisTemplate.opsForValue().get(key);

            //내 것인지 확인 후 삭제 → 다른 유저 것을 실수로 해제하는 것 방지
            if (String.valueOf(memberId).equals(savedValue)) {
                redisTemplate.delete(key);
            }
            // 내 것이 아니거나 이미 만료된 경우 → 아무것도 안 함 (정상 케이스)

        } catch (Exception e) {
            // Redis 장애 시 로그만 남기고 넘어감
            // 어차피 TTL 만료되면 자동 해제되므로 치명적이지 않음
            log.error("Redis 장애 발생 - 좌석 해제 실패. seatId={}, memberId={}", seatId, memberId, e);
        }
    }

    // 현재 좌석이 HOLD 상태인지 확인
    public boolean isHeld(Long seatId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(generateKey(seatId))); // Redis Key 존재 여부 확인
        } catch (Exception e) {
            // Redis 장애 시 AVAILABLE로 간주
            log.error("Redis 장애 발생 - 홀딩 여부 확인 실패. seatId={}", seatId, e);
            return false;
        }
    }

    // 현재 좌석을 선점한 사용자 조회
    public Long getHoldMemberId(Long seatId) {
        try {
            String value = redisTemplate.opsForValue().get(generateKey(seatId));
            return value != null ? Long.parseLong(value) : null;
        } catch (Exception e) {
            log.error("Redis 장애 발생 - 홀딩 유저 조회 실패. seatId={}", seatId, e);
            return null;
        }
    }
}
