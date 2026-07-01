package com.back.team9.moyeota.domain.seat.controller;

import com.back.team9.moyeota.domain.seat.dto.SeatLayoutResponse;
import com.back.team9.moyeota.domain.seat.dto.SeatResponse;
import com.back.team9.moyeota.domain.seat.service.SeatService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatController implements SeatControllerDocs {

    private final SeatService seatService;

    @GetMapping("/pathinfos/{pathId}/seats")
    public ResponseEntity<ApiResponse<SeatLayoutResponse>> getSeatLayout(
            @PathVariable("pathId") Long pathId,
            @AuthenticationPrincipal Long currentMemberId
    ) {
        SeatLayoutResponse response = seatService.getSeatLayout(pathId, currentMemberId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", response));
    }

    @PostMapping("/seats/{seatId}/hold")
    public ResponseEntity<ApiResponse<SeatResponse>> holdSeat(
            @PathVariable("seatId") Long seatId,
            @AuthenticationPrincipal Long currentMemberId
    ) {
        SeatResponse response = seatService.holdSeat(seatId, currentMemberId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", response));
    }
}
