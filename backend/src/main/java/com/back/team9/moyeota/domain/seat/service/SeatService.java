package com.back.team9.moyeota.domain.seat.service;

import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.seat.dto.SeatLayoutResponse;
import com.back.team9.moyeota.domain.seat.dto.SeatResponse;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatDisplayStatus;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;
    private final SeatRedisService seatRedisService;
    private final PathinfoRepository pathinfoRepository;

    @Transactional(readOnly = true)
    public SeatLayoutResponse getSeatLayout(Long pathId, Long currentMemberId) {

        Pathinfo pathinfo = pathinfoRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATH_NOT_FOUND));
        validateUsablePathinfo(pathinfo);

        List<Seat> seats = seatRepository.findByPathinfo_PathinfoId(pathId);

        List<Long> seatIds = seats.stream()
                .map(Seat::getSeatId)
                .toList();

        // Redis MGET으로 모든 좌석의 홀딩 정보를 한 번에 조회 (N+1 최적화)
        Map<Long, Long> holdMemberMap = seatRedisService.getHoldMemberIds(seatIds);

        // 좌석별 실제 표시 상태 계산 (DB + Redis 조합)
        List<SeatResponse> seatResponses = seats.stream()
                .map(seat -> {

                    // MGET으로 미리 조회한 Map에서 현재 좌석의 선점 유저 ID 확인
                    Long holdMemberId = holdMemberMap.get(seat.getSeatId());
                    boolean isHeld = holdMemberId != null;

                    SeatDisplayStatus displayStatus;

                    if (seat.getStatus() == SeatStatus.BOOKED) {
                        displayStatus = SeatDisplayStatus.BOOKED;
                    } else if (isHeld) {
                        displayStatus = SeatDisplayStatus.HOLD;
                    } else {
                        displayStatus = SeatDisplayStatus.AVAILABLE;
                    }
                    boolean mySeat = holdMemberId != null
                            && holdMemberId.equals(currentMemberId);

                    return SeatResponse.from(
                            seat,
                            displayStatus,
                            mySeat
                    );
                })
                .toList();

        return SeatLayoutResponse.from(
                pathId,
                pathinfo.getBusType().name(),
                seatResponses
        );
    }

    @Transactional(readOnly = true) // DB는 조회만 수행, 좌석 선점 상태는 Redis 저장
    public SeatResponse holdSeat(Long seatId, Long currentMemberId) {

        // 인증되지 않은 사용자는 좌석 선점 불가
        if (currentMemberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        Pathinfo pathinfo = seat.getPathinfo();
        if (pathinfo.getStatus() == PathinfoStatus.COMPLETED
                || pathinfo.getStatus() == PathinfoStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.PATH_INVALID_STATUS);
        }
        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }
        seatRedisService.holdSeat(
                seatId,
                currentMemberId
        );
        return SeatResponse.from(
                seat,
                SeatDisplayStatus.HOLD,
                true
        );
    }

    private void validateUsablePathinfo(Pathinfo pathinfo) {
        if (pathinfo.getStatus() == PathinfoStatus.COMPLETED
                || pathinfo.getStatus() == PathinfoStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.PATH_INVALID_STATUS);
        }
    }
}
