package com.back.team9.moyeota.domain.funding.policy;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class FundingPricePolicy {

    private static final BigDecimal ROUND_TRIP_MULTIPLIER = BigDecimal.valueOf(2);
    private static final BigDecimal PRICE_UNIT = BigDecimal.valueOf(100);

    // 지역1, 지역2, 버스타입, 총 금액
    private static final Map<PriceKey, BigDecimal> ONE_WAY_PRICES = Map.ofEntries(
            Map.entry(key(Region.SEOUL, Region.BUSAN, BusType.BUS_45), price(1742400)),
            Map.entry(key(Region.SEOUL, Region.BUSAN, BusType.BUS_25), price(1210000)),

            Map.entry(key(Region.SEOUL, Region.DAEJEON, BusType.BUS_45), price(774400)),
            Map.entry(key(Region.SEOUL, Region.DAEJEON, BusType.BUS_25), price(550000)),

            Map.entry(key(Region.SEOUL, Region.INCHEON, BusType.BUS_45), price(726000)),
            Map.entry(key(Region.SEOUL, Region.INCHEON, BusType.BUS_25), price(495000)),

            Map.entry(key(Region.SEOUL, Region.DAEGU, BusType.BUS_45), price(1369720)),
            Map.entry(key(Region.SEOUL, Region.DAEGU, BusType.BUS_25), price(959200)),

            Map.entry(key(Region.SEOUL, Region.GWANGJU, BusType.BUS_45), price(1452000)),
            Map.entry(key(Region.SEOUL, Region.GWANGJU, BusType.BUS_25), price(1016400)),

            Map.entry(key(Region.SEOUL, Region.ULSAN, BusType.BUS_45), price(1650440)),
            Map.entry(key(Region.SEOUL, Region.ULSAN, BusType.BUS_25), price(1155000)),

            Map.entry(key(Region.BUSAN, Region.DAEJEON, BusType.BUS_45), price(1056000)),
            Map.entry(key(Region.BUSAN, Region.DAEJEON, BusType.BUS_25), price(739200)),

            Map.entry(key(Region.BUSAN, Region.INCHEON, BusType.BUS_45), price(1848000)),
            Map.entry(key(Region.BUSAN, Region.INCHEON, BusType.BUS_25), price(1293600)),

            Map.entry(key(Region.BUSAN, Region.DAEGU, BusType.BUS_45), price(484000)),
            Map.entry(key(Region.BUSAN, Region.DAEGU, BusType.BUS_25), price(338800)),

            Map.entry(key(Region.BUSAN, Region.GWANGJU, BusType.BUS_45), price(1144000)),
            Map.entry(key(Region.BUSAN, Region.GWANGJU, BusType.BUS_25), price(800800)),

            Map.entry(key(Region.BUSAN, Region.ULSAN, BusType.BUS_45), price(363000)),
            Map.entry(key(Region.BUSAN, Region.ULSAN, BusType.BUS_25), price(254100)),

            Map.entry(key(Region.DAEJEON, Region.INCHEON, BusType.BUS_45), price(858000)),
            Map.entry(key(Region.DAEJEON, Region.INCHEON, BusType.BUS_25), price(600600)),

            Map.entry(key(Region.DAEJEON, Region.DAEGU, BusType.BUS_45), price(726000)),
            Map.entry(key(Region.DAEJEON, Region.DAEGU, BusType.BUS_25), price(508200)),

            Map.entry(key(Region.DAEJEON, Region.GWANGJU, BusType.BUS_45), price(748000)),
            Map.entry(key(Region.DAEJEON, Region.GWANGJU, BusType.BUS_25), price(523600)),

            Map.entry(key(Region.DAEJEON, Region.ULSAN, BusType.BUS_45), price(1034000)),
            Map.entry(key(Region.DAEJEON, Region.ULSAN, BusType.BUS_25), price(723800)),

            Map.entry(key(Region.INCHEON, Region.DAEGU, BusType.BUS_45), price(1452000)),
            Map.entry(key(Region.INCHEON, Region.DAEGU, BusType.BUS_25), price(1016400)),

            Map.entry(key(Region.INCHEON, Region.GWANGJU, BusType.BUS_45), price(1507000)),
            Map.entry(key(Region.INCHEON, Region.GWANGJU, BusType.BUS_25), price(1054900)),

            Map.entry(key(Region.INCHEON, Region.ULSAN, BusType.BUS_45), price(1749000)),
            Map.entry(key(Region.INCHEON, Region.ULSAN, BusType.BUS_25), price(1224300)),

            Map.entry(key(Region.DAEGU, Region.GWANGJU, BusType.BUS_45), price(935000)),
            Map.entry(key(Region.DAEGU, Region.GWANGJU, BusType.BUS_25), price(654500)),

            Map.entry(key(Region.DAEGU, Region.ULSAN, BusType.BUS_45), price(594000)),
            Map.entry(key(Region.DAEGU, Region.ULSAN, BusType.BUS_25), price(415800)),

            Map.entry(key(Region.GWANGJU, Region.ULSAN, BusType.BUS_45), price(1232000)),
            Map.entry(key(Region.GWANGJU, Region.ULSAN, BusType.BUS_25), price(862400))
    );

    private FundingPricePolicy() {
    }

    public static BigDecimal calculateTotalPrice(
            RouteRequest route,
            BusType busType,
            TripType tripType
    ) {
        if (route == null) {
            throw new BusinessException(ErrorCode.PATHINFO_REQUIRED);
        }

        if (busType == null
                || tripType == null) {
            throw new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION);
        }

        BigDecimal oneWayPrice = getOneWayPrice(
                route.departureRegion(),
                route.arrivalRegion(),
                busType
        );

        return tripType == TripType.ROUND
                ? oneWayPrice.multiply(ROUND_TRIP_MULTIPLIER)
                : oneWayPrice;
    }

    public static BigDecimal calculateTotalPrice(
            Region departureRegion,
            Region arrivalRegion,
            BusType busType,
            TripType tripType
    ) {
        if (tripType == null) {
            throw new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION);
        }

        BigDecimal oneWayPrice = getOneWayPrice(
                departureRegion,
                arrivalRegion,
                busType
        );

        return tripType == TripType.ROUND
                ? oneWayPrice.multiply(ROUND_TRIP_MULTIPLIER)
                : oneWayPrice;
    }

    private static BigDecimal getOneWayPrice(
            Region departureRegion,
            Region arrivalRegion,
            BusType busType
    ) {
        if (departureRegion == null
                || arrivalRegion == null
                || busType == null) {
            throw new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION);
        }

        if (departureRegion == arrivalRegion) {
            throw new BusinessException(ErrorCode.SAME_DEPARTURE_ARRIVAL);
        }

        BigDecimal price = ONE_WAY_PRICES.get(
                key(departureRegion, arrivalRegion, busType)
        );

        if (price == null) {
            throw new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION);
        }

        return price;
    }

    private static PriceKey key(
            Region departureRegion,
            Region arrivalRegion,
            BusType busType
    ) {
        return new PriceKey(
                EnumSet.of(departureRegion, arrivalRegion),
                busType
        );
    }

    private static BigDecimal price(long amount) {
        return BigDecimal.valueOf(amount);
    }

    // 인당 금액 PRICE_UNIT 단위 올림 처리
    public static BigDecimal calculateRoundedPrice(
            BigDecimal totalPrice,
            Integer participants
    ) {
        return totalPrice
                .divide(BigDecimal.valueOf(participants), 0, RoundingMode.CEILING)
                .divide(PRICE_UNIT, 0, RoundingMode.CEILING)
                .multiply(PRICE_UNIT);
    }

    private record PriceKey(
            Set<Region> route,
            BusType busType
    ) {
    }
}
