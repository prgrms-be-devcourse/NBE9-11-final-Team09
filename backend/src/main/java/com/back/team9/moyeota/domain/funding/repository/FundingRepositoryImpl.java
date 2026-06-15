package com.back.team9.moyeota.domain.funding.repository;


import com.back.team9.moyeota.domain.funding.dto.FundingSearchCondition;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

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
                .where(
                        pathinfo.direction.eq(Direction.OUTBOUND), // 가는 노선만(중복방지)
                        funding.status.in(condition.effectiveStatuses()), // 상태 필터
                        departureDateEq(condition.departureDate()),
                        departureRegionEq(condition.departureRegion()),
                        arrivalRegionEq(condition.arrivalRegion())
                )
                .offset(pageable.getOffset()) // 몇번부터 보여줄지
                .limit(pageable.getPageSize()) // 페이지 사이즈
                .orderBy(funding.departureDate.asc()) // 출발일 가까운순 정렬
                .fetch();

        // 조건에 맞는 펀딩 개수 조회
        Long total = queryFactory
                .select(funding.count())
                .from(funding)
                .join(pathinfo).on(pathinfo.funding.eq(funding))
                .where(
                        pathinfo.direction.eq(Direction.OUTBOUND),
                        funding.status.in(condition.effectiveStatuses()),
                        departureDateEq(condition.departureDate()),
                        departureRegionEq(condition.departureRegion()),
                        arrivalRegionEq(condition.arrivalRegion())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanExpression departureDateEq(LocalDate departureDate) {
        return departureDate == null
                ? null
                : funding.departureDate.eq(departureDate);
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
