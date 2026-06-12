package com.back.team9.moyeota.domain.pathinfo.service;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathinfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.pathinfo.validator.PathinfoValidator;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PathinfoService {

    private final PathinfoRepository pathinfoRepository;
    private final PathinfoValidator pathinfoValidator;

    @Transactional
    public void createPathinfos(
            Funding funding,
            TripType tripType,
            RouteRequest route
    ) {
        pathinfoValidator.validateCreateTripType(tripType, route);

        Pathinfo outbound = Pathinfo.create(
                funding,
                route.departureTime(),
                route.departureAddress(),
                route.departureRegion(),
                route.arrivalAddress(),
                route.arrivalRegion(),
                Direction.OUTBOUND
        );

        pathinfoRepository.save(outbound);

        if (tripType == TripType.ROUND) {
            Pathinfo returned = Pathinfo.create(
                    funding,
                    route.returnTime(),
                    route.arrivalAddress(),
                    route.arrivalRegion(),
                    route.departureAddress(),
                    route.departureRegion(),
                    Direction.RETURN
            );

            pathinfoRepository.save(returned);
        }
    }

    @Transactional
    public void updatePathinfos(
            Funding funding,
            TripType tripType,
            RouteRequest route
    ) {
        pathinfoValidator.validateUpdateTripType(tripType, route);

        Pathinfo outbound = pathinfoRepository
                .findByFunding_FundingIdAndDirection(
                        funding.getFundingId(),
                        Direction.OUTBOUND
                )
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

        Pathinfo returned = pathinfoRepository
                .findByFunding_FundingIdAndDirection(
                        funding.getFundingId(),
                        Direction.RETURN
                )
                .orElse(null);

        if (tripType == TripType.ROUND) {
            if (returned == null) {
                Pathinfo newReturn = Pathinfo.create(
                        funding,
                        route.returnTime(),
                        route.arrivalAddress(),
                        route.arrivalRegion(),
                        route.departureAddress(),
                        route.departureRegion(),
                        Direction.RETURN
                );

                pathinfoRepository.save(newReturn);
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
            returned.cancel();
        }
    }

    @Transactional(readOnly = true)
    public List<PathinfoResponse> getPathinfoResponses(Long fundingId) {
        return pathinfoRepository
                .findByFunding_FundingIdAndStatusNot(
                        fundingId,
                        PathinfoStatus.CANCELLED
                )
                .stream()
                .map(PathinfoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Pathinfo getFirstPathinfo(Long fundingId) {
        return pathinfoRepository
                .findByFunding_FundingIdAndDirectionAndStatusNot(
                        fundingId,
                        Direction.OUTBOUND,
                        PathinfoStatus.CANCELLED
                )
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Pathinfo> findByFundingId(Long fundingId) {

        return pathinfoRepository.findByFunding_FundingId(
                fundingId
        );
    }

    @Transactional
    public void cancelPathinfos(Long fundingId) {

        List<Pathinfo> pathinfos = pathinfoRepository.findByFunding_FundingId(fundingId);

        pathinfos.forEach(Pathinfo::cancel);
    }

    @Transactional
    public void syncBusType(Long fundingId, BusType busType) {
        pathinfoRepository.findByFunding_FundingId(fundingId).forEach(path -> path.changeBusType(busType));
    }

    @Transactional(readOnly = true)
    public List<Pathinfo> findByFunding_FundingIdInAndDirection(
            List<Long> fundingIds,
            Direction direction
    ) {
        return pathinfoRepository
                .findByFunding_FundingIdInAndDirectionAndStatusNot(
                        fundingIds,
                        direction,
                        PathinfoStatus.CANCELLED
                );
    }

    @Transactional(readOnly = true)
    public boolean isRouteChanged(
            Long fundingId,
            TripType tripType,
            RouteRequest route
    ) {
        List<Pathinfo> pathinfos =
                pathinfoRepository.findByFunding_FundingId(fundingId);
        Pathinfo outbound = pathinfos.stream()
                .filter(path -> path.getDirection() == Direction.OUTBOUND)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PATHINFO_REQUIRED));

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

        Pathinfo returned = pathinfos.stream()
                .filter(path -> path.getDirection() == Direction.RETURN)
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
