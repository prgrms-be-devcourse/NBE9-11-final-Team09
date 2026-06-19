package com.back.team9.moyeota.domain.seat.event;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.event.FundingCreatedEvent;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatFundingCreatedEventListener {
    private final PathinfoRepository pathinfoRepository;
    private final SeatRepository seatRepository;

    @EventListener
    @Transactional
    public void handleFundingCreated(FundingCreatedEvent event) {

        Long fundingId = event.funding().getFundingId();
        BusType busType = event.funding().getBusType();
        TripType tripType = event.funding().getTripType();

        log.info("펀딩 생성 이벤트 수신 - fundingId: {}, busType: {}, tripType: {}",
                fundingId, busType, tripType);

        // 해당 펀딩의 모든 노선 조회
        List<Pathinfo> pathinfos = pathinfoRepository
                .findByFunding_FundingId(fundingId);

        // 각 노선마다 좌석 생성
        for (Pathinfo pathinfo : pathinfos) {
            List<Seat> seats = createSeats(pathinfo, busType);
            seatRepository.saveAll(seats);

            log.info("좌석 생성 완료 - pathInfoId: {}, 좌석 수: {}",
                    pathinfo.getPathinfoId(), seats.size());
        }
    }

    // 버스 종류에 따라 좌석 목록 생성
    // BUS_25 → 1A~8C (24석)
    // BUS_45 → 1A~11D (44석)
    private List<Seat> createSeats(Pathinfo pathinfo, BusType busType) {
        // busType null 방어 코드
        if (busType == null) {
            throw new BusinessException(ErrorCode.INVALID_BUS_TYPE);
        }

        int maxRow;
        String[] columns;

        if (busType == BusType.BUS_25) {
            // 25인승: 8행 × 3열 (A, B, C) = 24석
            maxRow = 8;
            columns = new String[]{"A", "B", "C"};
        } else if (busType == BusType.BUS_45) {
            // 45인승: 11행 × 4열 (A, B, C, D) = 44석
            maxRow = 11;
            columns = new String[]{"A", "B", "C", "D"};
        } else {
            throw new BusinessException(ErrorCode.INVALID_BUS_TYPE);
        }

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= maxRow; row++) {
            for (String col : columns) {
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + col)
                        .build());
            }
        }

        return seats;
    }
}
