package com.back.team9.moyeota.domain.funding.policy;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class FundingPricePolicy {

    // 지역1, 지역2, 버스타입, 총 금액
    private static final Map<PriceKey, Integer> ONE_WAY_PRICES = Map.ofEntries(
            Map.entry(key(Region.SEOUL, Region.BUSAN, BusType.BUS_45), 1742400),
            Map.entry(key(Region.SEOUL, Region.BUSAN, BusType.BUS_25), 1210000),

            Map.entry(key(Region.SEOUL, Region.DAEJEON, BusType.BUS_45), 774400),
            Map.entry(key(Region.SEOUL, Region.DAEJEON, BusType.BUS_25), 550000),

            Map.entry(key(Region.SEOUL, Region.INCHEON, BusType.BUS_45), 726000),
            Map.entry(key(Region.SEOUL, Region.INCHEON, BusType.BUS_25), 495000),

            Map.entry(key(Region.SEOUL, Region.DAEGU, BusType.BUS_45), 1369720),
            Map.entry(key(Region.SEOUL, Region.DAEGU, BusType.BUS_25), 959200),

            Map.entry(key(Region.SEOUL, Region.GWANGJU, BusType.BUS_45), 1452000),
            Map.entry(key(Region.SEOUL, Region.GWANGJU, BusType.BUS_25), 1016400),

            Map.entry(key(Region.SEOUL, Region.ULSAN, BusType.BUS_45), 1650440),
            Map.entry(key(Region.SEOUL, Region.ULSAN, BusType.BUS_25), 1155000)
    );

    private FundingPricePolicy() {
    }

    public static int calculateTotalPrice(
            RouteRequest route,
            BusType busType,
            TripType tripType
    ) {
        int oneWayPrice = getOneWayPrice(
                route.departureRegion(),
                route.arrivalRegion(),
                busType
        );

        return tripType == TripType.ROUND
                ? oneWayPrice * 2
                : oneWayPrice;
    }

    private static int getOneWayPrice(
            Region departureRegion,
            Region arrivalRegion,
            BusType busType
    ) {
        if (departureRegion == arrivalRegion) {
            throw new BusinessException(ErrorCode.SAME_DEPARTURE_ARRIVAL);
        }

        Integer price = ONE_WAY_PRICES.get(
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

    private record PriceKey(
            Set<Region> route,
            BusType busType
    ) {
    }
}