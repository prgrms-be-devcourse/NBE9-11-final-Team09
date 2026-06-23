package com.back.team9.moyeota.domain.participation.controller;

import com.back.team9.moyeota.domain.participation.dto.MyParticipationResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ParticipationCreateRequest request
    ) {

        ParticipationResponse response =
                participationService.createParticipation(
                        memberId,
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
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long participationId
    ) {

        participationService.cancelParticipation(
                memberId,
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
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long fundingId
    ) {

        List<ParticipationListResponse> response =
                participationService.getParticipations(
                        memberId,
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

    // 내 참여 내역 조회
    @GetMapping("/participations/me")
    public ResponseEntity<ApiResponse<List<MyParticipationResponse>>> getMyParticipations(
            @AuthenticationPrincipal Long memberId
    ) {
        List<MyParticipationResponse> response =
                participationService.getMyParticipations(memberId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "내 참여 내역 조회가 완료되었습니다.",
                        response
                )
        );
    }
}
