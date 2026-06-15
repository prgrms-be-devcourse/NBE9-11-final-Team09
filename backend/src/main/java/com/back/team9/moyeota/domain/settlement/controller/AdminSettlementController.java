package com.back.team9.moyeota.domain.settlement.controller;

import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.service.SettlementService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settlements")
public class AdminSettlementController {

    private final SettlementService settlementService;

    @PatchMapping("/{settlementId}/approve")
    public ResponseEntity<ApiResponse<SettlementResponse>> approve(
            @PathVariable Long settlementId) {
        // TODO: JWT 연동 후 Admin 권한 검증 필요
        SettlementResponse response = settlementService.approve(settlementId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "페이백 승인이 완료되었습니다.", response));
    }

    @PatchMapping("/{settlementId}/reject")
    public ResponseEntity<ApiResponse<SettlementResponse>> reject(
            @PathVariable Long settlementId) {
        // TODO: JWT 연동 후 Admin 권한 검증 필요
        SettlementResponse response = settlementService.reject(settlementId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "페이백 거절이 완료되었습니다.", response));
    }
}
