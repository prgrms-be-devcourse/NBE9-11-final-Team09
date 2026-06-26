package com.back.team9.moyeota.domain.funding.controller;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.service.FundingService;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings")
public class FundingController implements FundingControllerDocs {

    private final FundingService fundingService;

    @PostMapping
    public ResponseEntity<ApiResponse<FundingCreateResponse>> createFunding(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid FundingCreateRequest request
    ) {
        FundingCreateResponse response =
                fundingService.createFunding(
                        memberId,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "펀딩 생성 성공",
                        response
                )
        );
    }

    @GetMapping("/{fundingId}")
    public ResponseEntity<ApiResponse<FundingDetailResponse>> getFunding(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long fundingId
    ) {
        FundingDetailResponse response =
                fundingService.getFunding(fundingId, memberId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "펀딩 조회 성공",
                        response
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FundingListResponse>>> getFundingList(
            @ModelAttribute FundingSearchCondition condition,
            @PageableDefault( // 디폴트 페이징 설정 (페이지당 20개, 가까운 출발일순)
                    size = 20,
                    sort = "departureDate",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        PageResponse<FundingListResponse> response =
                fundingService.getFundingList(condition, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "펀딩 목록 조회 성공",
                        response
                )
        );
    }

    @PatchMapping("/{fundingId}")
    public ResponseEntity<ApiResponse<Void>> updateFunding(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long fundingId,
            @RequestBody @Valid FundingUpdateRequest request
    ) {
        fundingService.updateFunding(
                memberId,
                fundingId,
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "펀딩 수정 성공"
                )
        );
    }

    @DeleteMapping("/{fundingId}")
    public ResponseEntity<ApiResponse<Void>> cancelFunding(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long fundingId
    ) {
        fundingService.cancelFunding(
                memberId,
                fundingId
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "펀딩 취소 성공"
                )
        );
    }
}
