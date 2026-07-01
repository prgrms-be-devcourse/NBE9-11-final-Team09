package com.back.team9.moyeota.domain.seat.controller;

import com.back.team9.moyeota.domain.seat.dto.SeatLayoutResponse;
import com.back.team9.moyeota.domain.seat.dto.SeatResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Seat", description = "버스 좌석 배치도 조회 및 Redis 기반 5분 선점 API")
public interface SeatControllerDocs {

    @Operation(
            summary = "버스 좌석 배치도 조회",
            description = "노선 ID로 해당 버스의 전체 좌석 배치도와 각 좌석의 선점 상태를 조회합니다."
    )
    ResponseEntity<ApiResponse<SeatLayoutResponse>> getSeatLayout(
            @Parameter(description = "조회할 노선 ID", required = true) @PathVariable("pathId") Long pathId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long currentMemberId
    );

    @Operation(
            summary = "좌석 선점 (5분 홀딩)",
            description = "좌석을 5분간 선점합니다. Redis SET NX EX를 사용하여 동시 선점을 방지합니다. 선점 후 5분 내에 결제를 완료하지 않으면 자동 해제됩니다."
    )
    ResponseEntity<ApiResponse<SeatResponse>> holdSeat(
            @Parameter(description = "선점할 좌석 ID", required = true) @PathVariable("seatId") Long seatId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long currentMemberId
    );
}
