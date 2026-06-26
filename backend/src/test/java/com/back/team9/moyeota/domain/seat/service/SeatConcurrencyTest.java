package com.back.team9.moyeota.domain.seat.service;

import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeatConcurrencyTest {

    @Autowired
    private SeatRedisService seatRedisService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 테스트용 고정값
    private static final Long TEST_SEAT_ID = 9999L;

    @AfterEach
    void tearDown() {
        // 매 테스트 후 Redis 키 정리 (다음 테스트에 영향 안 주도록!)
        redisTemplate.delete("seat:" + TEST_SEAT_ID);
    }

    @Test
    @DisplayName("10명이 동시에 같은 좌석 HOLD 시도 시 1명만 성공해야 한다")
    void 동시에_같은좌석_HOLD_시도시_1명만_성공() throws InterruptedException {

        int threadCount = 10;

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long memberId = i + 1L;

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 실제 Redis holdSeat() 동시 호출
                    seatRedisService.holdSeat(TEST_SEAT_ID, memberId);
                    successCount.incrementAndGet();

                } catch (BusinessException e) {
                    // 이미 다른 사용자가 Redis HOLD 중이면 BusinessException 발생 = 정상적인 실패
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        readyLatch.await();
        // 동시 출발
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // ===== 검증 =====

        // 성공은 정확히 1명
        assertThat(successCount.get())
                .as("HOLD 성공은 1명이어야 한다")
                .isEqualTo(1);

        // 나머지 9명은 실패
        assertThat(failCount.get())
                .as("나머지 9명은 실패해야 한다")
                .isEqualTo(threadCount - 1);

        // Redis에 키가 1개만 존재하는지 확인
        String redisValue = redisTemplate.opsForValue().get("seat:" + TEST_SEAT_ID);
        assertThat(redisValue)
                .as("Redis에 HOLD 키가 존재해야 한다")
                .isNotNull();

        // Redis 값이 1~10 중 하나인지 확인
        assertThat(redisValue)
                .as("Redis 값은 성공한 memberId 중 하나여야 한다")
                .isIn("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

        // TTL이 설정됐는지 확인 (SET NX EX 300 검증)
        Long ttl = redisTemplate.getExpire("seat:" + TEST_SEAT_ID, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(ttl)
                .as("TTL이 설정되어 있어야 한다")
                .isGreaterThan(0)
                .isLessThanOrEqualTo(300);

        System.out.println("성공!: " + successCount.get() + "명");
        System.out.println("실패ㅠ: " + failCount.get() + "명");
        System.out.println("Redis HOLD 값: " + redisValue);
    }
}
