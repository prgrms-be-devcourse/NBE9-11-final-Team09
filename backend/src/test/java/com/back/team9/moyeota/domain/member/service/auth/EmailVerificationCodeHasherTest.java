package com.back.team9.moyeota.domain.member.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이메일 인증코드 해시 테스트")
class EmailVerificationCodeHasherTest {

    private final EmailVerificationCodeHasher hasher =
            new EmailVerificationCodeHasher();

    @Test
    @DisplayName("인증코드를 SHA-256으로 해싱한다")
    void hashVerificationCodeReturnsSha256Hash() {
        String hash = hasher.hash("A1B2C3");

        assertThat(hash)
                .isNotEqualTo("A1B2C3")
                .hasSize(64);
    }

    @Test
    @DisplayName("동일한 인증코드이면 검증에 성공한다")
    void matchesWithSameCodeReturnsTrue() {
        String hash = hasher.hash("A1B2C3");

        assertThat(hasher.matches("A1B2C3", hash))
                .isTrue();
    }

    @Test
    @DisplayName("다른 인증코드이면 검증에 실패한다")
    void matchesWithDifferentCodeReturnsFalse() {
        String hash = hasher.hash("A1B2C3");

        assertThat(hasher.matches("WRONG1", hash))
                .isFalse();
    }

    @Test
    @DisplayName("저장된 해시 형식이 잘못되면 검증에 실패한다")
    void matchesWithInvalidHashReturnsFalse() {
        assertThat(hasher.matches("A1B2C3", "invalid-hash"))
                .isFalse();
    }

    @Test
    @DisplayName("인증코드 또는 해시가 없으면 검증에 실패한다")
    void matchesWithNullValueReturnsFalse() {
        assertThat(hasher.matches(null, "hash")).isFalse();
        assertThat(hasher.matches("A1B2C3", null)).isFalse();
    }
}