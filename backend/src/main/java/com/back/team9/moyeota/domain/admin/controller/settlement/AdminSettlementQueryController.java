package com.back.team9.moyeota.domain.admin.controller.settlement;

import com.back.team9.moyeota.domain.admin.dto.settlement.*;
import com.back.team9.moyeota.domain.admin.service.settlement.AdminSettlementQueryService;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settlements")
public class AdminSettlementQueryController {

    private final AdminSettlementQueryService adminSettlementQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminSettlementListResponse>>> getSettlements(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_GET_SETTLEMENTS_SUCCESS",
                "정산 내역 조회에 성공했습니다.",
                adminSettlementQueryService.getSettlements(pageable)
        ));
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<AdminSettlementDetailResponse>> getSettlement(
            @PathVariable Long settlementId
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_GET_SETTLEMENT_SUCCESS",
                "정산 상세 조회에 성공했습니다.",
                adminSettlementQueryService.getSettlement(settlementId)
        ));
    }
}
