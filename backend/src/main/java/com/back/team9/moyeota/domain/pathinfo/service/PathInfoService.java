package com.back.team9.moyeota.domain.pathinfo.service;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.domain.pathinfo.repository.PathInfoRepository;
import com.back.team9.moyeota.domain.pathinfo.validator.PathInfoValidator;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PathInfoService {

    private final PathInfoRepository pathInfoRepository;
    private final PathInfoValidator pathInfoValidator;

    @Transactional
    public void createPathInfos(
            Funding funding,
            TripType tripType,
            RouteRequest route
    ) {
        pathInfoValidator.validateCreateTripType(tripType, route);

        PathInfo outbound = PathInfo.create(
                funding,
                route.departureTime(),
                route.departureAddress(),
                route.departureRegion(),
                route.arrivalAddress(),
                route.arrivalRegion(),
                Direction.OUTBOUND
        );

        pathInfoRepository.save(outbound);

        if (tripType == TripType.ROUND) {
            PathInfo returned = PathInfo.create(
                    funding,
                    route.returnTime(),
                    route.arrivalAddress(),
                    route.arrivalRegion(),
                    route.departureAddress(),
                    route.departureRegion(),
                    Direction.RETURN
            );

            pathInfoRepository.save(returned);
        }
    }

    @Transactional
    public void updatePathInfos(
            Funding funding,
            TripType tripType,
            RouteRequest route
    ) {
        pathInfoValidator.validateUpdateTripType(tripType, route);

        List<PathInfo> existingPaths =
                pathInfoRepository.findByFunding_FundingId(
                        funding.getFundingId()
                );

        PathInfo outbound = existingPaths.stream()
                .filter(path -> path.getDirection() == Direction.OUTBOUND)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PATHINFO_REQUIRED
                ));

        outbound.update(
                route.departureTime(),
                route.departureAddress(),
                route.departureRegion(),
                route.arrivalAddress(),
                route.arrivalRegion(),
                Direction.OUTBOUND
        );

        PathInfo returned = existingPaths.stream()
                .filter(path -> path.getDirection() == Direction.RETURN)
                .findFirst()
                .orElse(null);

        if (tripType == TripType.ROUND) {
            if (returned == null) {
                PathInfo newReturn = PathInfo.create(
                        funding,
                        route.returnTime(),
                        route.arrivalAddress(),
                        route.arrivalRegion(),
                        route.departureAddress(),
                        route.departureRegion(),
                        Direction.RETURN
                );

                pathInfoRepository.save(newReturn);
                return;
            }

            returned.update(
                    route.returnTime(),
                    route.arrivalAddress(),
                    route.arrivalRegion(),
                    route.departureAddress(),
                    route.departureRegion(),
                    Direction.RETURN
            );

            return;
        }

        if (returned != null) {
            pathInfoRepository.delete(returned);
        }
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

    @Transactional
    public List<PathInfo> findByFunding_FundingIdInAndDirection(
            List<Long> fundingIds,
            Direction direction
    ) {
        return pathInfoRepository.findByFunding_FundingIdInAndDirection(fundingIds, direction);
    }

    @Transactional(readOnly = true)
    public boolean isRouteChanged(
            Long fundingId,
            TripType tripType,
            RouteRequest route
    ) {

        PathInfo outbound = pathInfoRepository
                .findByFunding_FundingId(fundingId)
                .stream()
                .filter(path ->
                        path.getDirection() == Direction.OUTBOUND
                )
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PATHINFO_REQUIRED
                ));

        boolean outboundChanged =
                !Objects.equals(
                        outbound.getDepartureTime(),
                        route.departureTime()
                )
                        || !Objects.equals(
                        outbound.getDepartureAddress(),
                        route.departureAddress()
                )
                        || !Objects.equals(
                        outbound.getDepartureRegion(),
                        route.departureRegion()
                )
                        || !Objects.equals(
                        outbound.getArrivalAddress(),
                        route.arrivalAddress()
                )
                        || !Objects.equals(
                        outbound.getArrivalRegion(),
                        route.arrivalRegion()
                );

        if (outboundChanged) {
            return true;
        }

        PathInfo returned = pathInfoRepository
                .findByFunding_FundingId(fundingId)
                .stream()
                .filter(path ->
                        path.getDirection() == Direction.RETURN
                )
                .findFirst()
                .orElse(null);

        if (tripType == TripType.ONE_WAY) {
            return route.returnTime() != null;
        }

        return returned == null
                || !Objects.equals(
                returned.getDepartureTime(),
                route.returnTime()
        );
    }
}
