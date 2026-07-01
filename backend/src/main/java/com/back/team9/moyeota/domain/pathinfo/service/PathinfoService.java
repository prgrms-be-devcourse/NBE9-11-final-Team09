package com.back.team9.moyeota.domain.pathinfo.service;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.dto.PathinfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.pathinfo.validator.PathinfoTimeValidator;
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
    private final PathinfoTimeValidator pathinfoTimeValidator;

    public void validatePathinfoRequest(
            TripType tripType,
            RouteRequest route
    ) {
        PathinfoValidator.validateTripType(tripType, route);
        pathinfoTimeValidator.validateDepartureDate(route.departureTime());
    }

    // 펀딩 생성 시 노선 생성
    @Transactional
    public void createPathinfos(
            Funding funding,
            TripType tripType,
            RouteRequest route
    ) {
        validatePathinfoRequest(tripType, route);

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

    // 펀딩 수정 시 노선 수정
    // 편도->왕복: RETURN 생성 or 수정
    // 왕복->편도: RETURN CANCELLED처리
    @Transactional
    public void updatePathinfos(
            Funding funding,
            TripType tripType,
            RouteRequest route
    ) {
        validatePathinfoRequest(tripType, route);

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
            // 왕복으로 수정 -> 오는 노선 아예 없으면 추가
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

            // 왕복으로 수정 -> 오는 노선 있으면 수정
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

    // 상세 조회용 노선 응답 조회
    // 취소 노선 제외하고 유효한 노선만 반환
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

    // 취소/실패 펀딩 조회시 왕복/편도 조건에 따라 가져올 노선
    @Transactional(readOnly = true)
    public List<PathinfoResponse> getPathinfoResponsesForDetail(Funding funding) {
        if (funding.getStatus() == FundingStatus.CANCELLED
                || funding.getStatus() == FundingStatus.FAILED) {
            return getPathinfoResponsesByTripType(funding);
        }

        return getPathinfoResponses(funding.getFundingId());
    }

    // 여러 펀딩의 특정 방향 노선 일괄 조회
    @Transactional(readOnly = true)
    public List<Pathinfo> findByFundingIdsAndDirection(
            List<Long> fundingIds,
            Direction direction
    ) {
        return pathinfoRepository.findByFunding_FundingIdInAndDirection(
                fundingIds,
                direction
        );
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

    // 노선 취소 공통
    @Transactional
    public void cancelPathinfos(Long fundingId) {

        List<Pathinfo> pathinfos = pathinfoRepository.findByFunding_FundingId(fundingId);

        pathinfos.forEach(Pathinfo::cancel);
    }

    // 노선의 버스타입 펀도의 버스타입으로 가져옴
    @Transactional
    public void syncBusType(Long fundingId, BusType busType) {
        pathinfoRepository.findByFunding_FundingId(fundingId).forEach(path -> path.changeBusType(busType));
    }

    // 노선 정보가 달라졌는지 검사
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

    // 편도면 가는 노선만, 왕복이면 오는 노선도 조회
    private List<PathinfoResponse> getPathinfoResponsesByTripType(Funding funding) {
        List<Pathinfo> pathinfos = pathinfoRepository.findByFunding_FundingId(
                funding.getFundingId()
        );

        return pathinfos.stream()
                .filter(pathinfo -> {
                    if (funding.getTripType() == TripType.ONE_WAY) {
                        return pathinfo.getDirection() == Direction.OUTBOUND;
                    }

                    return pathinfo.getDirection() == Direction.OUTBOUND
                            || pathinfo.getDirection() == Direction.RETURN;
                })
                .map(PathinfoResponse::from)
                .toList();
    }
}
