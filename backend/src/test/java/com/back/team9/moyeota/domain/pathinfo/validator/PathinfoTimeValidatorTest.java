package com.back.team9.moyeota.domain.pathinfo.validator;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathinfoTimeValidatorTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-22T00:00:00Z"),
            SEOUL
    );

    private final PathinfoTimeValidator pathinfoTimeValidator =
            new PathinfoTimeValidator(CLOCK);

    @Test
    @DisplayName("출발일 검증 - 현재 시각 기준 14일 이후면 통과한다")
    void validateDepartureDate_whenAfterMinimumDate_doesNotThrow() {
        // Given
        LocalDateTime departureTime = LocalDateTime.now(CLOCK)
                .plusDays(14);

        // When / Then
        assertThatCode(() ->
                pathinfoTimeValidator.validateDepartureDate(departureTime)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("출발일 검증 - 현재 시각 기준 14일 이전이면 예외")
    void validateDepartureDate_whenBeforeMinimumDate_throwsException() {
        // Given
        LocalDateTime departureTime = LocalDateTime.now(CLOCK)
                .plusDays(13);

        // When / Then
        assertThatThrownBy(() ->
                pathinfoTimeValidator.validateDepartureDate(departureTime)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTURE_DATE_TOO_SOON);
    }

    @Test
    @DisplayName("출발일 검증 - 출발 시간이 없으면 예외")
    void validateDepartureDate_whenDepartureTimeIsNull_throwsException() {
        // When / Then
        assertThatThrownBy(() ->
                pathinfoTimeValidator.validateDepartureDate(null)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }
}
