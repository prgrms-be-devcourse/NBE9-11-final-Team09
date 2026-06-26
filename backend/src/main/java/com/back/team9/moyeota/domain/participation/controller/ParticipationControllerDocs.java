package com.back.team9.moyeota.domain.participation.controller;

import com.back.team9.moyeota.domain.participation.dto.MyParticipationResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Participation", description = "펀딩 참여 신청·취소 및 참여자 목록 조회 API")
public interface ParticipationControllerDocs {

    @Operation(
            summary = "참여 신청",
            description = "펀딩에 참여를 신청합니다. 신청 성공 시 좌석이 임시 배정되며 보증금 결제(DEPOSIT) 단계로 진입합니다."
    )
    ResponseEntity<ApiResponse<ParticipationResponse>> createParticipation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ParticipationCreateRequest request
    );

    @Operation(
            summary = "참여 취소",
            description = "자신의 참여를 취소합니다. 이미 보증금 결제가 완료된 경우 환불 정책에 따라 결제가 취소됩니다."
    )
    ResponseEntity<ApiResponse<Void>> cancelParticipation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "취소할 참여 ID", required = true) @PathVariable Long participationId
    );

    @Operation(
            summary = "펀딩 참여자 목록 조회 (방장용)",
            description = "방장이 자신의 펀딩에 참여한 회원 목록을 조회합니다. 방장 본인만 접근 가능합니다."
    )
    ResponseEntity<ApiResponse<List<ParticipationListResponse>>> getParticipations(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "조회할 펀딩 ID", required = true) @PathVariable Long fundingId
    );

    @Operation(
            summary = "내 참여 내역 조회",
            description = "로그인한 회원 자신의 전체 참여 내역을 조회합니다."
    )
    ResponseEntity<ApiResponse<List<MyParticipationResponse>>> getMyParticipations(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );
}
