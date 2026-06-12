package com.back.team9.moyeota.domain.pathinfo.validator;

import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoCreateRequest;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoUpdateRequest;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PathInfoValidator {

    public void validateDepartureDate(LocalDateTime departureTime) {

        if (departureTime.isBefore(LocalDateTime.now().plusDays(14))) {
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

    public void validateCreateTripType(TripType tripType, List<PathInfoCreateRequest> paths) {

        if (paths == null || paths.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PATHINFO_REQUIRED
            );
        }

        for (PathInfoCreateRequest path : paths) {
            validateRegion(path.departureRegion(), path.arrivalRegion());
            validateDepartureDate(path.departureTime());
        }



        boolean hasOutbound = paths.stream()
                .anyMatch(path -> path.direction() == Direction.OUTBOUND);

        boolean hasReturn = paths.stream()
                .anyMatch(path -> path.direction() == Direction.RETURN);

        if (tripType == TripType.ONE_WAY) {
            if (paths.size() != 1 || !hasOutbound) {
                throw new BusinessException(
                        ErrorCode.INVALID_PATH_CONFIGURATION
                );
            }
        }

        if (tripType == TripType.ROUND) {

            if (paths.size() != 2
                    || !hasOutbound
                    || !hasReturn) {
                throw new BusinessException(
                        ErrorCode.INVALID_PATH_CONFIGURATION
                );
            }

            PathInfoCreateRequest outbound = paths.stream()
                    .filter(path -> path.direction() == Direction.OUTBOUND)
                    .findFirst()
                    .orElseThrow();

            PathInfoCreateRequest returned =
                    paths.stream()
                            .filter(path -> path.direction() == Direction.RETURN)
                            .findFirst()
                            .orElseThrow();

            validateRoundTripTime(
                    outbound.departureTime(),
                    returned.departureTime()
            );
        }


    }

    public void validateUpdateTripType(TripType tripType, List<PathInfoUpdateRequest> paths) {

        if (paths == null || paths.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PATHINFO_REQUIRED
            );
        }

        for (PathInfoUpdateRequest path : paths) {

            validateRegion(path.departureRegion(), path.arrivalRegion());
            validateDepartureDate(path.departureTime());
        }


        boolean hasOutbound = paths.stream()
                .anyMatch(path -> path.direction() == Direction.OUTBOUND);

        boolean hasReturn = paths.stream()
                .anyMatch(path -> path.direction() == Direction.RETURN);

        if (tripType == TripType.ONE_WAY) {
            if (paths.size() != 1 || !hasOutbound) {
                throw new BusinessException(
                        ErrorCode.INVALID_PATH_CONFIGURATION
                );
            }
        }

        if (tripType == TripType.ROUND) {

            if (paths.size() != 2
                    || !hasOutbound
                    || !hasReturn) {

                throw new BusinessException(
                        ErrorCode.INVALID_PATH_CONFIGURATION
                );
            }
            PathInfoUpdateRequest outbound =
                    paths.stream()
                            .filter(path -> path.direction() == Direction.OUTBOUND)
                            .findFirst()
                            .orElseThrow();

            PathInfoUpdateRequest returned =
                    paths.stream()
                            .filter(path -> path.direction() == Direction.RETURN)
                            .findFirst()
                            .orElseThrow();

            validateRoundTripTime(
                    outbound.departureTime(),
                    returned.departureTime()
            );
        }
    }
}
