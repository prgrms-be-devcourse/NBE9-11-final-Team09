package com.back.team9.moyeota.domain.admin.controller;

import com.back.team9.moyeota.domain.admin.dto.AdminFundingCancelRequest;
import com.back.team9.moyeota.domain.admin.dto.AdminFundingCancelResponse;
import com.back.team9.moyeota.domain.admin.dto.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.service.AdminFundingService;
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
@RequestMapping("/api/admin/fundings")
public class AdminFundingController {

    private final AdminFundingService adminFundingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminFundingListResponse>>> getFundings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_GET_FUNDINGS_SUCCESS",
                "펀딩 목록 조회에 성공했습니다.",
                adminFundingService.getFundings(pageable)
        ));
    }

    @PatchMapping("/{fundingId}")
    public ResponseEntity<ApiResponse<AdminFundingCancelResponse>> cancelFunding(
            @PathVariable Long fundingId,
            @Valid @RequestBody AdminFundingCancelRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_CANCEL_FUNDING_SUCCESS",
                "펀딩이 강제 취소되었습니다.",
                adminFundingService.cancelFunding(fundingId, request)
        ));
    }
}