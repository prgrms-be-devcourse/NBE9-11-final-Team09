package com.back.team9.moyeota.domain.funding.policy;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundingPricePolicyTest {

    private static final LocalDateTime DEPARTURE_TIME =
            LocalDateTime.of(2027, 6, 20, 8, 0);

    @Test
    @DisplayName("금액 계산 - 편도 금액을 계산한다")
    void calculateTotalPrice_whenOneWay_returnsOneWayPrice() {
        // Given
        RouteRequest route = route(Region.SEOUL, Region.BUSAN);

        // When
        BigDecimal result = FundingPricePolicy.calculateTotalPrice(
                route,
                BusType.BUS_45,
                TripType.ONE_WAY
        );

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1742400));
    }

    @Test
    @DisplayName("금액 계산 - 왕복이면 편도 금액의 2배를 반환한다")
    void calculateTotalPrice_whenRound_returnsDoubleOneWayPrice() {
        // Given
        RouteRequest route = route(Region.SEOUL, Region.BUSAN);

        // When
        BigDecimal result = FundingPricePolicy.calculateTotalPrice(
                route,
                BusType.BUS_45,
                TripType.ROUND
        );

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(3484800));
    }

    @Test
    @DisplayName("금액 계산 - 노선 정보가 없으면 예외")
    void calculateTotalPrice_whenRouteIsNull_throwsException() {
        // When / Then
        assertThatThrownBy(() ->
                FundingPricePolicy.calculateTotalPrice(
                        null,
                        BusType.BUS_45,
                        TripType.ONE_WAY
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PATHINFO_REQUIRED);
    }

    @Test
    @DisplayName("금액 계산 - 버스 타입이 없으면 예외")
    void calculateTotalPrice_whenBusTypeIsNull_throwsException() {
        // Given
        RouteRequest route = route(Region.SEOUL, Region.BUSAN);

        // When / Then
        assertThatThrownBy(() ->
                FundingPricePolicy.calculateTotalPrice(
                        route,
                        null,
                        TripType.ONE_WAY
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    @Test
    @DisplayName("금액 계산 - 가격표에 없는 지역 조합이면 예외")
    void calculateTotalPrice_whenPriceIsNotConfigured_throwsException() {
        // Given
        RouteRequest route = route(Region.INCHEON, Region.BUSAN);

        // When / Then
        assertThatThrownBy(() ->
                FundingPricePolicy.calculateTotalPrice(
                        route,
                        BusType.BUS_45,
                        TripType.ONE_WAY
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    private RouteRequest route(
            Region departureRegion,
            Region arrivalRegion
    ) {
        return new RouteRequest(
                DEPARTURE_TIME,
                null,
                "Departure",
                departureRegion,
                "Arrival",
                arrivalRegion
        );
    }
}
