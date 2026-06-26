package com.back.team9.moyeota.domain.settlement.controller;

import com.back.team9.moyeota.domain.settlement.dto.SettlementCreateRequest;
import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Settlement", description = "방장 페이백 정산 요청·조회 API")
public interface SettlementControllerDocs {

    @Operation(
            summary = "정산 내역 생성",
            description = "펀딩이 COMPLETED 상태일 때 방장이 정산을 요청합니다. 클라이언트 금액을 신뢰하지 않고 DB에서 PAID 결제 합산액을 기준으로 수수료 및 페이백 금액을 계산합니다."
    )
    ResponseEntity<ApiResponse<SettlementResponse>> create(
            @RequestBody @Valid SettlementCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );

    @Operation(
            summary = "펀딩별 정산 내역 조회",
            description = "방장이 자신의 펀딩에 대한 정산 내역을 조회합니다."
    )
    ResponseEntity<ApiResponse<SettlementResponse>> getByFundingId(
            @Parameter(description = "조회할 펀딩 ID", required = true) @PathVariable Long fundingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );
}
