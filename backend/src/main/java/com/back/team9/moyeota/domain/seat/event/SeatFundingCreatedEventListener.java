package com.back.team9.moyeota.domain.seat.event;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.event.FundingCreatedEvent;
import com.back.team9.moyeota.domain.funding.event.FundingSeatsRecreateEvent;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    @EventListener
    @Transactional
    public void handleFundingCreated(FundingCreatedEvent event) {

        Long fundingId = event.funding().getFundingId();
        BusType busType = event.funding().getBusType();
        TripType tripType = event.funding().getTripType();

        log.info("펀딩 생성 이벤트 수신 - fundingId: {}, busType: {}, tripType: {}",
                fundingId, busType, tripType);

        List<Pathinfo> pathinfos = pathinfoRepository
                .findByFunding_FundingId(fundingId);

        for (Pathinfo pathinfo : pathinfos) {
            List<Seat> seats = createSeatsWithHostSeat(
                    pathinfo,
                    pathinfo.getBusType(),
                    event.funding().getMember(),
                    hostSeatNumber(
                            pathinfo.getDirection(),
                            event.hostOutboundSeatNumber(),
                            event.hostReturnSeatNumber()
                    )
            );
            seatRepository.saveAll(seats);

            log.info("좌석 생성 완료 - pathInfoId: {}, 좌석 수: {}",
                    pathinfo.getPathinfoId(), seats.size());
        }
    }

    @EventListener
    @Transactional
    public void handleFundingSeatsRecreate(FundingSeatsRecreateEvent event) {
        Long fundingId = event.funding().getFundingId();
        Long hostMemberId = event.funding().getMember().getMemberId();

        log.info("좌석 재생성 이벤트 수신 - fundingId: {}", fundingId);

        List<Pathinfo> pathinfos = pathinfoRepository
                .findByFunding_FundingId(fundingId);

        for (Pathinfo pathinfo : pathinfos) {
            Long pathinfoId = pathinfo.getPathinfoId();
            PathinfoStatus status = pathinfo.getStatus();
            BusType busType = pathinfo.getBusType();
            Direction direction = pathinfo.getDirection();

            seatRepository.deleteByPathinfo_PathinfoId(pathinfoId);

            if (status == PathinfoStatus.CANCELLED) {
                log.info("취소 노선 좌석 삭제 완료 - pathInfoId: {}", pathinfo.getPathinfoId());
                continue;
            }

            Pathinfo seatPathinfo = pathinfoRepository.getReferenceById(pathinfoId);
            Member hostMember = entityManager.getReference(Member.class, hostMemberId);
            List<Seat> seats = createSeatsWithHostSeat(
                    seatPathinfo,
                    busType,
                    hostMember,
                    hostSeatNumber(
                            direction,
                            event.hostOutboundSeatNumber(),
                            event.hostReturnSeatNumber()
                    )
            );
            seatRepository.saveAll(seats);

            log.info("좌석 재생성 완료 - pathInfoId: {}, 좌석 수: {}",
                    pathinfo.getPathinfoId(), seats.size());
        }
    }

    // 버스 종류에 따라 좌석 목록 생성
    private List<Seat> createSeats(Pathinfo pathinfo, BusType busType) {
        // busType null 방어 코드
        if (busType == null) {
            throw new BusinessException(ErrorCode.INVALID_BUS_TYPE);
        }

        int maxRow;
        String[] columns;

        if (busType == BusType.BUS_25) {
            maxRow = 8;
            columns = new String[]{"A", "B", "C"};
        } else if (busType == BusType.BUS_45) {
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

    private List<Seat> createSeatsWithHostSeat(
            Pathinfo pathinfo,
            BusType busType,
            Member hostMember,
            String hostSeatNumber
    ) {
        List<Seat> seats = createSeats(pathinfo, busType);
        Seat hostSeat = seats.stream()
                .filter(seat -> seat.getSeatNumber().equals(hostSeatNumber))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        hostSeat.bookByHost(hostMember);
        return seats;
    }

    private String hostSeatNumber(
            Direction direction,
            String outboundSeatNumber,
            String returnSeatNumber
    ) {
        if (direction == Direction.OUTBOUND) {
            if (isBlank(outboundSeatNumber)) {
                throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
            }
            return outboundSeatNumber;
        }

        if (isBlank(returnSeatNumber)) {
            throw new BusinessException(ErrorCode.ROUND_TRIP_SEAT_REQUIRED);
        }
        return returnSeatNumber;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
