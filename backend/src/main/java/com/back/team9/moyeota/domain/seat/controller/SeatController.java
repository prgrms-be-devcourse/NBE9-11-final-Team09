package com.back.team9.moyeota.domain.seat.controller;

import com.back.team9.moyeota.domain.seat.dto.SeatLayoutResponse;
import com.back.team9.moyeota.domain.seat.dto.SeatResponse;
import com.back.team9.moyeota.domain.seat.service.SeatService;
import com.back.team9.moyeota.global.response.ApiResponse;
// TODO: Swagger 설정 팀이랑 맞춘 후 @Tag, @Operation 주석 해제
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@Tag(name = "Seat", description = "좌석 관련 API") // Swagger 좌석 API 그룹
@RestController // REST API 컨트롤러
@RequestMapping("/api") // 공통 URL prefix
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class SeatController {

    private final SeatService seatService; // 좌석 비즈니스 로직 호출

    // TODO: Swagger 설정 팀이랑 맞춘 후 @Tag, @Operation 주석 해제
    //@Operation(summary = "특정 버스 좌석 배치도 조회") // Swagger API 설명
    @GetMapping("/pathinfos/{pathId}/seats") // GET /api/pathinfos/{pathId}/seats
    public ResponseEntity<ApiResponse<SeatLayoutResponse>> getSeatLayout(
            @PathVariable("pathId") Long pathId // 조회할 노선 ID
    ) {

        Long currentMemberId = 1L; //TODO: 임시 테스트용 회원 ID (인증 연동 후 제거)

        // 좌석 배치도 조회
        SeatLayoutResponse response =
                seatService.getSeatLayout(pathId, currentMemberId);

        // 공통 응답 포맷 반환
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", response)
        );
    }

    // TODO: Swagger 설정 팀이랑 맞춘 후 @Tag, @Operation 주석 해제
    //@Operation(summary = "좌석 선점 (5분 홀딩)") // Swagger API 설명
    @PostMapping("/seats/{seatId}/hold") // POST /api/seats/{seatId}/hold
    public ResponseEntity<ApiResponse<SeatResponse>> holdSeat(
            @PathVariable("seatId") Long seatId // 선점할 좌석 ID
    ) {

        Long currentMemberId = 1L; //TODO: 임시 테스트용 회원 ID (인증 연동 후 제거)

        // Redis SET NX EX로 5분 선점
        SeatResponse response =
                seatService.holdSeat(seatId, currentMemberId);

        // 공통 응답 포맷 반환
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "요청이 성공했습니다.", response)
        );
    }
}
