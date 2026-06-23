package com.back.team9.moyeota.domain.pathinfo.validator;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PathinfoValidator {

    private final Clock clock;

    public void validateDepartureDate(LocalDateTime departureTime) {

        if (departureTime.isBefore(LocalDateTime.now(clock).plusDays(14))) {
            throw new BusinessException(
                    ErrorCode.DEPARTURE_DATE_TOO_SOON
            );
        }
    }

    public void validateRoundTripTime(LocalDateTime outboundTime, LocalDateTime returnTime) {

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

    public void validateRegion(Region departureRegion, Region arrivalRegion) {

        if (departureRegion == arrivalRegion) {
            throw new BusinessException(
                    ErrorCode.SAME_DEPARTURE_ARRIVAL
            );
        }
    }

    public void validateTripType(
            TripType tripType,
            RouteRequest route
    ) {
        validateRoute(tripType, route);
    }

    private void validateRoute(
            TripType tripType,
            RouteRequest route
    ) {
        if (route == null) {
            throw new BusinessException(ErrorCode.PATHINFO_REQUIRED);
        }

        validateRegion(
                route.departureRegion(),
                route.arrivalRegion()
        );

        validateDepartureDate(route.departureTime());

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
