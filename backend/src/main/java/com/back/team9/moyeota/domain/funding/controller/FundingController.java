package com.back.team9.moyeota.domain.funding.controller;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.service.FundingService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings")
public class FundingController {

    private final FundingService fundingService;

    // 펀딩 생성
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

    // 펀딩 상세 조회
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

    // 펀딩 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<FundingListResponse>>> getFundingList() {
        List<FundingListResponse> response =
                fundingService.getFundingList();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "펀딩 목록 조회 성공",
                        response
                )
        );
    }

    // 펀딩 수정
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

    // 펀딩 취소
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

