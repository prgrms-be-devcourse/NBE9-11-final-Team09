package com.back.team9.moyeota.domain.seat.event;

import com.back.team9.moyeota.domain.funding.event.FundingSeatsRecreateEvent;
import com.back.team9.moyeota.domain.pathinfo.event.PathinfoCancelledEvent;
import com.back.team9.moyeota.domain.pathinfo.event.PathinfoCreatedEvent;
import com.back.team9.moyeota.domain.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatEventListener {

    private final SeatService seatService;

    @EventListener
    public void createSeats(PathinfoCreatedEvent event) {
        try {
            seatService.createSeatsForPathinfo(event.pathinfo());
        } catch (RuntimeException e) {
            log.error(
                    "Failed to create seats. pathinfoId: {}",
                    event.pathinfo().getPathinfoId(),
                    e
            );
            throw e;
        }
    }

    @EventListener
    public void deleteSeats(PathinfoCancelledEvent event) {
        try {
            seatService.deleteSeatsForPathinfo(event.pathinfo());
        } catch (RuntimeException e) {
            log.error(
                    "Failed to delete seats. pathinfoId: {}",
                    event.pathinfo().getPathinfoId(),
                    e
            );
            throw e;
        }
    }

    @EventListener
    public void recreateSeats(FundingSeatsRecreateEvent event) {
        try {
            seatService.recreateSeatsForActivePathinfos(event.fundingId());
        } catch (RuntimeException e) {
            log.error(
                    "Failed to recreate seats. fundingId: {}",
                    event.fundingId(),
                    e
            );
            throw e;
        }
    }
}
