package com.back.team9.moyeota.domain.participation.controller;

import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    // 참여 신청
    @PostMapping("/participations")
    public ResponseEntity<ApiResponse<ParticipationResponse>> createParticipation(
            @Valid @RequestBody ParticipationCreateRequest request
    ) {

        // TODO: JWT 연동 후 현재 로그인 회원 ID 사용
        Long currentMemberId = 1L;

        ParticipationResponse response =
                participationService.createParticipation(
                        currentMemberId,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "참여 신청이 완료되었습니다.",
                        response
                )
        );
    }

    // 참여 취소
    @DeleteMapping("/participations/{participationId}")
    public ResponseEntity<ApiResponse<Void>> cancelParticipation(
            @PathVariable Long participationId
    ) {

        Long currentMemberId = 1L; // TODO: 임시 테스트용 회원 ID (인증 연동 후 제거)

        participationService.cancelParticipation(
                currentMemberId,
                participationId
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "참여 취소가 완료되었습니다."
                )
        );
    }

    // 참여자 목록 조회 (방장용)
    @GetMapping("/fundings/{fundingId}/participations")
    public ResponseEntity<ApiResponse<List<ParticipationListResponse>>> getParticipations(
            @PathVariable Long fundingId
    ) {

        // TODO: JWT 연동 후 현재 로그인 회원 ID 사용
        Long currentMemberId = 1L;

        List<ParticipationListResponse> response =
                participationService.getParticipations(
                        currentMemberId,
                        fundingId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "참여자 목록 조회가 완료되었습니다.",
                        response
                )
        );
    }
}
