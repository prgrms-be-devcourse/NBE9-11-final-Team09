package com.back.team9.moyeota.domain.settlement.controller;

import com.back.team9.moyeota.domain.settlement.dto.SettlementCreateRequest;
import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.service.SettlementService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<ApiResponse<SettlementResponse>> create(
            @RequestBody @Valid SettlementCreateRequest request) {
        SettlementResponse response = settlementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SUCCESS", "정산 내역이 생성되었습니다.", response));
    }

    @GetMapping("/funding/{fundingId}")
    public ResponseEntity<ApiResponse<SettlementResponse>> getByFundingId(
            @PathVariable Long fundingId) {
        SettlementResponse response = settlementService.getByFundingId(fundingId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "정산 내역을 조회했습니다.", response));
    }

}
