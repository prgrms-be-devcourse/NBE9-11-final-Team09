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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings")
public class FundingController {

    private final FundingService fundingService;

    @PostMapping
    public ResponseEntity<ApiResponse<FundingCreateResponse>> createFunding(
            @RequestBody @Valid FundingCreateRequest request
    ) {
        FundingCreateResponse response =
                fundingService.createFunding(
                        1L, // TODO JWT 연동
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
            @PathVariable Long fundingId
    ) {
        FundingDetailResponse response =
                fundingService.getFunding(fundingId);

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

    @PutMapping("/{fundingId}")
    public ResponseEntity<ApiResponse<Void>> updateFunding(
            @PathVariable Long fundingId,
            @RequestBody @Valid FundingUpdateRequest request
    ) {
        fundingService.updateFunding(
                1L, // TODO JWT 연동
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
            @PathVariable Long fundingId
    ) {
        fundingService.cancelFunding(
                1L, // TODO JWT 연동
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
