package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.history.MemberFundingResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberParticipationResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberPaymentResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Member History", description = "회원 내역 조회 API")
public interface MemberHistoryControllerDocs {

    @Operation(
            summary = "내 참여 내역 (페이지)",
            description = "내가 참여한 펀딩 목록을 페이지 단위로 조회합니다. 기본 정렬: 최신순, 페이지당 10건."
    )
    ResponseEntity<ApiResponse<PageResponse<MemberParticipationResponse>>> getMyParticipations(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            Pageable pageable
    );

    @Operation(
            summary = "내 모집 내역 (페이지)",
            description = "내가 방장으로 생성한 펀딩 목록을 페이지 단위로 조회합니다. 기본 정렬: 최신순, 페이지당 10건."
    )
    ResponseEntity<ApiResponse<PageResponse<MemberFundingResponse>>> getMyFundings(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            Pageable pageable
    );

    @Operation(
            summary = "내 결제 내역 (페이지)",
            description = "내 결제 내역을 페이지 단위로 조회합니다. PAID·REFUNDED 등 모든 상태의 결제가 포함됩니다. 기본 정렬: 최신순, 페이지당 10건."
    )
    ResponseEntity<ApiResponse<PageResponse<MemberPaymentResponse>>> getMyPayments(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            Pageable pageable
    );
}
