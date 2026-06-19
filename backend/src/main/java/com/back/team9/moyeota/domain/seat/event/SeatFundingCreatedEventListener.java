package com.back.team9.moyeota.domain.seat.event;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.event.FundingCreatedEvent;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
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

        List<Seat> seats = new ArrayList<>();

        if (busType == BusType.BUS_25) {
            for (int row = 1; row <= 8; row++) {
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "A")
                        .build());
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "B")
                        .build());
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "C")
                        .build());
            }
        } else if (busType == BusType.BUS_45) {
            for (int row = 1; row <= 11; row++) {
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "A")
                        .build());
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "B")
                        .build());
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "C")
                        .build());
                seats.add(Seat.builder()
                        .pathinfo(pathinfo)
                        .seatNumber(row + "D")
                        .build());
            }
        }

        return seats;
    }
}
