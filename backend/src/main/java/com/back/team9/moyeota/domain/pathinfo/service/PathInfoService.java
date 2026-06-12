package com.back.team9.moyeota.domain.pathinfo.service;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoCreateRequest;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoResponse;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoUpdateRequest;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathInfoRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PathInfoService {

    private final PathInfoRepository pathInfoRepository;

    @Transactional
    public void createPathInfos(
            Funding funding,
            TripType tripType,
            List<PathInfoCreateRequest> requests
    ) {

        validateCreateTripType(tripType, requests);
        List<PathInfo> pathInfos = requests.stream()
                .map(request -> PathInfo.create(
                        funding,
                        request.departureTime(),
                        request.departureAddress(),
                        request.departureRegion(),
                        request.arrivalAddress(),
                        request.arrivalRegion(),
                        request.direction()
                )).toList();

        pathInfoRepository.saveAll(pathInfos);
    }

    @Transactional
    public void updatePathInfos(
            Funding funding,
            TripType tripType,
            List<PathInfoUpdateRequest> requests
    ) {
        validateUpdateTripType(tripType, requests);
        List<PathInfo> existingPaths = pathInfoRepository.findByFunding_FundingId(funding.getFundingId());

        for (PathInfoUpdateRequest request : requests) {

            PathInfo existing = existingPaths.stream()
                    .filter(path -> path.getDirection() == request.direction())
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.update(
                        request.departureTime(),
                        request.departureAddress(),
                        request.departureRegion(),
                        request.arrivalAddress(),
                        request.arrivalRegion(),
                        request.direction()
                );

            } else {
                PathInfo newPath = PathInfo.create(
                        funding,
                        request.departureTime(),
                        request.departureAddress(),
                        request.departureRegion(),
                        request.arrivalAddress(),
                        request.arrivalRegion(),
                        request.direction()
                );

                pathInfoRepository.save(newPath);
            }
        }

        existingPaths.stream()
                .filter(path -> requests.stream().noneMatch(request -> request.direction() == path.getDirection()))
                .forEach(pathInfoRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PathInfoResponse> getPathInfoResponses(Long fundingId) {

        return pathInfoRepository
                .findByFunding_FundingId(fundingId)
                .stream()
                .map(PathInfoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PathInfo getFirstPathInfo(Long fundingId) {

        return pathInfoRepository
                .findByFunding_FundingId(fundingId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PathInfo> findByFundingId(Long fundingId) {

        return pathInfoRepository.findByFunding_FundingId(
                fundingId
        );
    }

    @Transactional
    public void cancelPathInfos(Long fundingId) {

        List<PathInfo> pathInfos = pathInfoRepository.findByFunding_FundingId(fundingId);

        pathInfos.forEach(PathInfo::cancel);
    }

    @Transactional
    public void syncBusType(Long fundingId, BusType busType) {
        pathInfoRepository.findByFunding_FundingId(fundingId).forEach(path -> path.changeBusType(busType));
    }


    private void validateDepartureDate(LocalDateTime departureTime) {

        if (departureTime.isBefore(LocalDateTime.now().plusDays(14))) {
            throw new BusinessException(
                    ErrorCode.DEPARTURE_DATE_TOO_SOON
            );
        }
    }

    private void validateRoundTripTime(LocalDateTime outboundTime, LocalDateTime returnTime) {

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

    private void validateRegion(Region departureRegion, Region arrivalRegion) {

        if (departureRegion == arrivalRegion) {
            throw new BusinessException(
                    ErrorCode.SAME_DEPARTURE_ARRIVAL
            );
        }
    }

    private void validateCreateTripType(TripType tripType, List<PathInfoCreateRequest> paths) {

        for (PathInfoCreateRequest path : paths) {
            validateRegion(path.departureRegion(), path.arrivalRegion());
            validateDepartureDate(path.departureTime());
        }

        if (paths == null || paths.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PATHINFO_REQUIRED
            );
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

    private void validateUpdateTripType(TripType tripType, List<PathInfoUpdateRequest> paths) {
        for (PathInfoUpdateRequest path : paths) {

            validateRegion(path.departureRegion(), path.arrivalRegion());
            validateDepartureDate(path.departureTime());
        }
        if (paths == null || paths.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PATHINFO_REQUIRED
            );
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
