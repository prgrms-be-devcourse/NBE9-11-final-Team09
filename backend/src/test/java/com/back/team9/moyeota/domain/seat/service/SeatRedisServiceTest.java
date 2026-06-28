package com.back.team9.moyeota.domain.seat.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatRedisServiceTest {

    @Autowired
    private SeatRedisService seatRedisService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long TEST_SEAT_ID = 8888L;
    private static final Long OWNER_ID = 1L; // 좌석 선점한 사람
    private static final Long STRANGER_ID = 2L; // 타인

    @AfterEach
    void tearDown() {
        // 매 테스트 후 Redis 키 정리
        redisTemplate.delete("seat:" + TEST_SEAT_ID);
    }

    @Test
    @DisplayName("본인이 HOLD 해제 시도 시 성공해야 한다")
    void releaseSeat_본인이_해제시도_성공() {

        // given
        seatRedisService.holdSeat(TEST_SEAT_ID, OWNER_ID);

        // when
        boolean result = seatRedisService.releaseSeat(TEST_SEAT_ID, OWNER_ID);

        // then
        assertThat(result).isTrue();

        // Redis 키도 삭제됐는지 확인
        String redisValue = redisTemplate.opsForValue().get("seat:" + TEST_SEAT_ID);
        assertThat(redisValue).isNull();
    }

    @Test
    @DisplayName("타인이 HOLD 해제 시도 시 실패해야 한다 (Lua 스크립트 검증)")
    void releaseSeat_타인이_해제시도_실패() {

        // given
        seatRedisService.holdSeat(TEST_SEAT_ID, OWNER_ID);

        // when
        boolean result = seatRedisService.releaseSeat(TEST_SEAT_ID, STRANGER_ID);

        // then
        assertThat(result).isFalse();

        String redisValue = redisTemplate.opsForValue().get("seat:" + TEST_SEAT_ID);
        assertThat(redisValue)
                .as("타인이 해제 시도해도 Redis 키는 유지되어야 한다")
                .isEqualTo(String.valueOf(OWNER_ID));
    }

    @Test
    @DisplayName("존재하지 않는 HOLD 키 해제 시도 시 실패해야 한다")
    void releaseSeat_없는키_해제시도_실패() {

        // when
        boolean result = seatRedisService.releaseSeat(TEST_SEAT_ID, OWNER_ID);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isHeldBy() - 본인이 HOLD 중인지 확인")
    void isHeldBy_본인_HOLD_확인() {

        // given
        seatRedisService.holdSeat(TEST_SEAT_ID, OWNER_ID);

        // when & then
        assertThat(seatRedisService.isHeldBy(TEST_SEAT_ID, OWNER_ID)).isTrue();
        assertThat(seatRedisService.isHeldBy(TEST_SEAT_ID, STRANGER_ID)).isFalse();
    }
}
