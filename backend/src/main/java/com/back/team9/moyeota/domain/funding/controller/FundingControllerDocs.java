package com.back.team9.moyeota.domain.funding.controller;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Funding", description = "버스 대절 펀딩 모집 생성·조회·수정·취소 API")
public interface FundingControllerDocs {

    @Operation(
            summary = "펀딩 생성",
            description = "방장이 버스 대절 펀딩을 생성합니다. 출발지·목적지·출발일·좌석 수·보증금·잔액 등 정보를 입력합니다."
    )
    ResponseEntity<ApiResponse<FundingCreateResponse>> createFunding(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid FundingCreateRequest request
    );

    @Operation(
            summary = "펀딩 상세 조회",
            description = "펀딩 ID로 상세 정보를 조회합니다. 인증된 사용자의 참여 여부 및 좌석 정보도 함께 반환합니다."
    )
    ResponseEntity<ApiResponse<FundingDetailResponse>> getFunding(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "조회할 펀딩 ID", required = true) @PathVariable Long fundingId
    );

    @Operation(
            summary = "펀딩 목록 조회",
            description = "출발지·목적지·출발일 등 검색 조건으로 펀딩 목록을 페이지 단위로 조회합니다. 기본 정렬: 출발일 오름차순, 페이지당 20건."
    )
    ResponseEntity<ApiResponse<PageResponse<FundingListResponse>>> getFundingList(
            @ModelAttribute FundingSearchCondition condition,
            Pageable pageable
    );

    @Operation(
            summary = "펀딩 수정",
            description = "방장이 자신의 펀딩 정보를 수정합니다. 이미 참여자가 있는 경우 일부 필드는 수정이 제한될 수 있습니다."
    )
    ResponseEntity<ApiResponse<Void>> updateFunding(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "수정할 펀딩 ID", required = true) @PathVariable Long fundingId,
            @RequestBody @Valid FundingUpdateRequest request
    );

    @Operation(
            summary = "펀딩 취소",
            description = "방장이 자신의 펀딩을 취소합니다. 펀딩 취소 시 참여한 모든 회원의 결제가 자동 환불 처리됩니다."
    )
    ResponseEntity<ApiResponse<Void>> cancelFunding(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Parameter(description = "취소할 펀딩 ID", required = true) @PathVariable Long fundingId
    );
}
