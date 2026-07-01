package com.back.team9.moyeota.domain.pathinfo.validator;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;

import java.time.LocalDateTime;

public final class PathinfoValidator {

    private PathinfoValidator() {
    }

    public static void validateRoundTripTime(LocalDateTime outboundTime, LocalDateTime returnTime) {

        if (!returnTime.isAfter(outboundTime)) {
            throw new BusinessException(
                    ErrorCode.RETURN_TIME_BEFORE_OUTBOUND
            );
        }

        if (!outboundTime.toLocalDate().equals(returnTime.toLocalDate())) {
            throw new BusinessException(
                    ErrorCode.RETURN_DATE_MUST_MATCH_OUTBOUND
            );
        }
    }

    public static void validateRegion(Region departureRegion, Region arrivalRegion) {

        if (departureRegion == arrivalRegion) {
            throw new BusinessException(
                    ErrorCode.SAME_DEPARTURE_ARRIVAL
            );
        }
    }

    public static void validateTripType(
            TripType tripType,
            RouteRequest route
    ) {
        validateRoute(tripType, route);
    }

    private static void validateRoute(
            TripType tripType,
            RouteRequest route
    ) {
        if (route == null) {
            throw new BusinessException(ErrorCode.PATHINFO_REQUIRED);
        }

        if (tripType == null
                || route.departureTime() == null
                || route.departureRegion() == null
                || route.arrivalRegion() == null) {
            throw new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION);
        }

        validateRegion(
                route.departureRegion(),
                route.arrivalRegion()
        );

        if (tripType == TripType.ONE_WAY) {
            if (route.returnTime() != null) {
                throw new BusinessException(
                        ErrorCode.INVALID_PATH_CONFIGURATION
                );
            }
            return;
        }

        if (tripType == TripType.ROUND) {
            if (route.returnTime() == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_PATH_CONFIGURATION
                );
            }

            validateRoundTripTime(
                    route.departureTime(),
                    route.returnTime()
            );
        }


    }
}
