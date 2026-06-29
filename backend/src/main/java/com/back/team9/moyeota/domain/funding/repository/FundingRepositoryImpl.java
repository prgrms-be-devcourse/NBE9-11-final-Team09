package com.back.team9.moyeota.domain.funding.repository;


import com.back.team9.moyeota.domain.funding.dto.FundingSearchCondition;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.back.team9.moyeota.domain.funding.entity.QFunding.funding;
import static com.back.team9.moyeota.domain.pathinfo.entity.QPathinfo.pathinfo;

@RequiredArgsConstructor
public class FundingRepositoryImpl implements FundingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Funding> findPageByCondition(
            FundingSearchCondition condition,
            Pageable pageable
    ) {
        List<Funding> content = queryFactory
                .selectFrom(funding)
                .join(pathinfo).on(pathinfo.funding.eq(funding)) // 펀딩 노선 연결
                .join(funding.member).fetchJoin()
                .where(whereConditions(condition))
                .offset(pageable.getOffset()) // 몇번부터 보여줄지
                .limit(pageable.getPageSize()) // 페이지 사이즈
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .fetch();

        // 조건에 맞는 펀딩 개수 조회
        Long total = queryFactory
                .select(funding.count())
                .from(funding)
                .join(pathinfo).on(pathinfo.funding.eq(funding))
                .where(whereConditions(condition))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // Pageable 정렬 조건 QueryDSL에 맞게 변환
    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        if (sort.isUnsorted()) {
            return new OrderSpecifier[]{funding.departureDate.asc()};
        }

        OrderSpecifier<?>[] orderSpecifiers = sort.stream()
                .map(this::toOrderSpecifier)
                .filter(Objects::nonNull)
                .toArray(OrderSpecifier[]::new);

        return orderSpecifiers.length == 0
                ? new OrderSpecifier[]{funding.departureDate.asc()}
                : orderSpecifiers;
    }

    // 들어온 정렬 필드 매핑
    private OrderSpecifier<?> toOrderSpecifier(Sort.Order order) {
        boolean ascending = order.isAscending();

        return switch (order.getProperty()) {
            case "fundingId" -> ascending
                    ? funding.fundingId.asc()
                    : funding.fundingId.desc();
            case "title" -> ascending
                    ? funding.title.asc()
                    : funding.title.desc();
            case "departureDate" -> ascending
                    ? funding.departureDate.asc()
                    : funding.departureDate.desc();
            case "status" -> ascending
                    ? funding.status.asc()
                    : funding.status.desc();
            case "busType" -> ascending
                    ? funding.busType.asc()
                    : funding.busType.desc();
            case "minParticipants" -> ascending
                    ? funding.minParticipants.asc()
                    : funding.minParticipants.desc();
            case "maxParticipants" -> ascending
                    ? funding.maxParticipants.asc()
                    : funding.maxParticipants.desc();
            case "paybackHold" -> ascending
                    ? funding.paybackHold.asc()
                    : funding.paybackHold.desc();
            case "tripType" -> ascending
                    ? funding.tripType.asc()
                    : funding.tripType.desc();
            case "totalPrice" -> ascending
                    ? funding.totalPrice.asc()
                    : funding.totalPrice.desc();
            case "createdAt" -> ascending
                    ? funding.createdAt.asc()
                    : funding.createdAt.desc();
            case "updatedAt" -> ascending
                    ? funding.updatedAt.asc()
                    : funding.updatedAt.desc();
            default -> null;
        };
    }

    private BooleanExpression[] whereConditions(
            FundingSearchCondition condition
    ) {
        return new BooleanExpression[]{
                pathinfo.direction.eq(Direction.OUTBOUND), // 가는 노선만
                funding.status.in(condition.effectiveStatuses()), // 상태 필터
                tripTypeEq(condition.tripType()), // 편도/왕복 필터
                departureDateEq(condition.departureDate()), // 출발일 필터
                departureRegionEq(condition.departureRegion()), // 출발지 필터
                arrivalRegionEq(condition.arrivalRegion()) // 도착지 필터
        };
    }

    private BooleanExpression departureDateEq(LocalDate departureDate) {
        return departureDate == null
                ? null
                : funding.departureDate.eq(departureDate);
    }

    private BooleanExpression tripTypeEq(TripType tripType) {
        return tripType == null
                ? null
                : funding.tripType.eq(tripType);
    }

    private BooleanExpression departureRegionEq(Region departureRegion) {
        return departureRegion == null
                ? null
                : pathinfo.departureRegion.eq(departureRegion);
    }

    private BooleanExpression arrivalRegionEq(Region arrivalRegion) {
        return arrivalRegion == null
                ? null
                : pathinfo.arrivalRegion.eq(arrivalRegion);
    }
}