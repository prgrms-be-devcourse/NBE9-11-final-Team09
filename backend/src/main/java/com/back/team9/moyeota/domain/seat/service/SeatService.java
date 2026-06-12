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

@Service // 좌석 도메인 비즈니스 로직 담당
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class SeatService {
    private final SeatRepository seatRepository; // 좌석 DB 조회
    private final SeatRedisService seatRedisService; // 좌석 HOLD Redis 처리
    private final PathinfoRepository pathinfoRepository; // 노선 조회

    @Transactional(readOnly = true) // 조회 전용 트랜잭션
    public SeatLayoutResponse getSeatLayout(Long pathid, Long currentMemberId) {

        // 노선 존재 여부 확인
        Pathinfo pathinfo = pathinfoRepository.findById(pathid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATH_NOT_FOUND));

        // 해당 노선의 전체 좌석 DB 조회
        List<Seat> seats = seatRepository.findByPathinfoPathinfoId(pathid);

        // 전체 좌석 ID 목록 추출
        List<Long> seatIds = seats.stream()
                .map(Seat::getSeatId)
                .toList();

        // Redis MGET으로 모든 좌석의 홀딩 정보를 한 번에 조회 (N+1 최적화)
        // 기존: 좌석마다 isHeld() + getHoldMemberId() → Redis 왕복 N×2번
        // 개선: getHoldMemberIds() 한 번 → Redis 왕복 1번
        Map<Long, Long> holdMemberMap = seatRedisService.getHoldMemberIds(seatIds);

        // 좌석별 실제 표시 상태 계산 (DB + Redis 조합)
        List<SeatResponse> seatResponses = seats.stream()
                .map(seat -> {

                    // MGET으로 미리 조회한 Map에서 현재 좌석의 선점 유저 ID 확인
                    // null이면 AVAILABLE, 값 있으면 HOLD 중
                    Long holdMemberId = holdMemberMap.get(seat.getSeatId());
                    boolean isHeld = holdMemberId != null;

                    SeatDisplayStatus displayStatus;

                    // 결제 완료 좌석
                    if (seat.getStatus() == SeatStatus.BOOKED) {
                        displayStatus = SeatDisplayStatus.BOOKED;

                        // Redis HOLD 중인 좌석
                    } else if (isHeld) {
                        displayStatus = SeatDisplayStatus.HOLD;

                        // 선택 가능한 좌석
                    } else {
                        displayStatus = SeatDisplayStatus.AVAILABLE;
                    }

                    // mySeat 계산: 내가 선점한 좌석인지 확인
                    // 현재 로그인한 사용자가 선점한 좌석인지 확인
                    boolean mySeat = holdMemberId != null
                            && holdMemberId.equals(currentMemberId);

                    return SeatResponse.from(
                            seat,
                            displayStatus,
                            mySeat
                    );
                })
                .toList();

        // 전체 좌석 배치도 응답 반환
        return SeatLayoutResponse.from(
                pathid,
                "TEMP", // TODO: Funding의 busType 연결 예정
                seatResponses
        );
    }

    @Transactional(readOnly = true) // DB 변경 없음 (Redis만 사용)
    public SeatResponse holdSeat(Long seatId, Long currentMemberId) {

        // 좌석 존재 여부 확인
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        // 노선 상태 확인
        // TODO: PathinfoStatus에 CANCELLED 추가 후 CANCELLED 체크 로직 추가 필요
        Pathinfo pathinfo = seat.getPathinfo();

        // 운행 완료 노선은 선점 불가
        if (pathinfo.getStatus() == PathinfoStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PATH_INVALID_STATUS);
        }

        // 이미 예약 완료된 좌석은 선점 불가 (DB 상태 확인)
        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        // Redis SET NX EX로 5분 선점
        // 이미 다른 유저가 선점 중이면 내부에서 SEAT_ALREADY_OCCUPIED 예외 발생
        seatRedisService.holdSeat(
                seatId,
                currentMemberId
        );

        // 선점 성공 응답 반환 (mySeat = true, 내가 방금 선점했으니까)
        return SeatResponse.from(
                seat,
                SeatDisplayStatus.HOLD,
                true
        );
    }
}
