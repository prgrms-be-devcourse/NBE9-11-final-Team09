package com.back.team9.moyeota.domain.pathinfo.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoCreateRequest;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoResponse;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoUpdateRequest;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.domain.pathinfo.repository.PathInfoRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        validateCreateTripType(
                tripType,
                requests
        );
        List<PathInfo> pathInfos = requests.stream()
                .map(request -> PathInfo.create(
                        funding,
                        request.departureTime(),
                        request.departureAddress(),
                        request.departureRegion(),
                        request.arrivalAddress(),
                        request.arrivalRegion(),
                        request.direction()
                ))
                .toList();

        pathInfoRepository.saveAll(pathInfos);
    }

    @Transactional
    public void updatePathInfos(
            Funding funding,
            TripType tripType,
            List<PathInfoUpdateRequest> requests
    ) {
        validateUpdateTripType(
                tripType,
                requests
        );
        List<PathInfo> existingPaths =
                pathInfoRepository.findByFunding_FundingId(
                        funding.getFundingId()
                );

        for (PathInfoUpdateRequest request : requests) {

            PathInfo existing = existingPaths.stream()
                    .filter(path ->
                            path.getDirection() == request.direction())
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
                .filter(path ->
                        requests.stream()
                                .noneMatch(request ->
                                        request.direction() == path.getDirection()))
                .forEach(pathInfoRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PathInfoResponse> getPathInfoResponses(
            Long fundingId
    ) {

        return pathInfoRepository
                .findByFunding_FundingId(fundingId)
                .stream()
                .map(PathInfoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PathInfo getFirstPathInfo(
            Long fundingId
    ) {

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

        List<PathInfo> pathInfos =
                pathInfoRepository.findByFunding_FundingId(
                        fundingId
                );

        pathInfos.forEach(PathInfo::cancel);
    }


    private void validateCreateTripType(
            TripType tripType,
            List<PathInfoCreateRequest> paths
    ) {

        if (paths == null || paths.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PATHINFO_REQUIRED
            );
        }

        boolean hasOutbound = paths.stream()
                .anyMatch(path ->
                        path.direction() == Direction.OUTBOUND);

        boolean hasReturn = paths.stream()
                .anyMatch(path ->
                        path.direction() == Direction.RETURN);

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
        }
    }
    private void validateUpdateTripType(
            TripType tripType,
            List<PathInfoUpdateRequest> paths
    ) {

        if (paths == null || paths.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PATHINFO_REQUIRED
            );
        }

        boolean hasOutbound = paths.stream()
                .anyMatch(path ->
                        path.direction() == Direction.OUTBOUND);

        boolean hasReturn = paths.stream()
                .anyMatch(path ->
                        path.direction() == Direction.RETURN);

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
        }
    }
}
