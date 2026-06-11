package com.back.team9.moyeota.domain.funding.controller;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.service.FundingService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings")
public class FundingController {

    private final FundingService fundingService;

    // 펀딩 생성
    @PostMapping
    public ApiResponse<FundingCreateResponse> createFunding(
            @RequestBody FundingCreateRequest request
    ) {

        FundingCreateResponse response =
                fundingService.createFunding(
                        1L, // TODO JWT 연동
                        request
                );

        return new ApiResponse<>(
                "SUCCESS",
                "펀딩 생성 성공",
                response
        );
    }

    // 펀딩 상세 조회
    @GetMapping("/{fundingId}")
    public ApiResponse<FundingDetailResponse> getFunding(
            @PathVariable Long fundingId
    ) {

        return new ApiResponse<>(
                "SUCCESS",
                "펀딩 조회 성공",
                fundingService.getFunding(
                        fundingId
                )
        );
    }

    // 펀딩 목록 조회
    @GetMapping
    public ApiResponse<List<FundingListResponse>> getFundingList() {

        return new ApiResponse<>(
                "SUCCESS",
                "펀딩 목록 조회 성공",
                fundingService.getFundingList()
        );
    }

    // 펀딩 수정
    @PutMapping("/{fundingId}")
    public ApiResponse<Void> updateFunding(
            @PathVariable Long fundingId,
            @RequestBody FundingUpdateRequest request
    ) {

        fundingService.updateFunding(
                fundingId,
                request
        );

        return new ApiResponse<>(
                "SUCCESS",
                "펀딩 수정 성공"
        );
    }

    // 펀딩 취소
    @DeleteMapping("/{fundingId}")
    public ApiResponse<Void> cancelFunding(
            @PathVariable Long fundingId
    ) {

        fundingService.cancelFunding(
                fundingId
        );

        return new ApiResponse<>(
                "SUCCESS",
                "펀딩 취소 성공"
        );
    }
}
