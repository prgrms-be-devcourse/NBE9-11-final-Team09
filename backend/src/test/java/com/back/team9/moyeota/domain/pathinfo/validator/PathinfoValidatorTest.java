package com.back.team9.moyeota.domain.pathinfo.validator;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathinfoValidatorTest {

    private static final LocalDateTime DEPARTURE_TIME =
            LocalDateTime.of(2027, 6, 20, 8, 0);
    private static final LocalDateTime RETURN_TIME =
            LocalDateTime.of(2027, 6, 20, 23, 0);

    @Test
    @DisplayName("노선 검증 - 편도 노선 형식이 올바르면 통과한다")
    void validateTripType_whenOneWayRouteIsValid_doesNotThrow() {
        // Given
        RouteRequest route = oneWayRoute();

        // When / Then
        assertThatCode(() ->
                PathinfoValidator.validateTripType(TripType.ONE_WAY, route)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("노선 검증 - 노선 정보가 없으면 예외")
    void validateTripType_whenRouteIsNull_throwsException() {
        // When / Then
        assertThatThrownBy(() ->
                PathinfoValidator.validateTripType(TripType.ONE_WAY, null)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PATHINFO_REQUIRED);
    }

    @Test
    @DisplayName("노선 검증 - 왕복 여부가 없으면 예외")
    void validateTripType_whenTripTypeIsNull_throwsException() {
        // Given
        RouteRequest route = oneWayRoute();

        // When / Then
        assertThatThrownBy(() ->
                PathinfoValidator.validateTripType(null, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    @Test
    @DisplayName("노선 검증 - 출발 지역과 도착 지역이 같으면 예외")
    void validateTripType_whenSameRegion_throwsException() {
        // Given
        RouteRequest route = new RouteRequest(
                DEPARTURE_TIME,
                null,
                "Seoul",
                Region.SEOUL,
                "Seoul Stadium",
                Region.SEOUL
        );

        // When / Then
        assertThatThrownBy(() ->
                PathinfoValidator.validateTripType(TripType.ONE_WAY, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_DEPARTURE_ARRIVAL);
    }

    @Test
    @DisplayName("노선 검증 - 편도인데 복귀 시간이 있으면 예외")
    void validateTripType_whenOneWayHasReturnTime_throwsException() {
        // Given
        RouteRequest route = new RouteRequest(
                DEPARTURE_TIME,
                RETURN_TIME,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL
        );

        // When / Then
        assertThatThrownBy(() ->
                PathinfoValidator.validateTripType(TripType.ONE_WAY, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    @Test
    @DisplayName("노선 검증 - 왕복인데 복귀 시간이 없으면 예외")
    void validateTripType_whenRoundHasNoReturnTime_throwsException() {
        // Given
        RouteRequest route = oneWayRoute();

        // When / Then
        assertThatThrownBy(() ->
                PathinfoValidator.validateTripType(TripType.ROUND, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    private RouteRequest oneWayRoute() {
        return new RouteRequest(
                DEPARTURE_TIME,
                null,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL
        );
    }
}
