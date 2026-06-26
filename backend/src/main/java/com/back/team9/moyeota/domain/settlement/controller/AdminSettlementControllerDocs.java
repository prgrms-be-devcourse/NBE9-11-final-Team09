package com.back.team9.moyeota.domain.settlement.controller;

import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Admin Settlement", description = "관리자 정산 관련 API")
public interface AdminSettlementControllerDocs {

    @Operation(
            summary = "페이백 승인",
            description = "관리자가 방장의 페이백 요청을 승인합니다. 정산 상태가 APPROVED로 변경되고 페이백 지급일이 기록됩니다."
    )
    ResponseEntity<ApiResponse<SettlementResponse>> approve(
            @Parameter(description = "승인할 정산 ID", required = true) @PathVariable Long settlementId
    );

    @Operation(
            summary = "페이백 거절",
            description = "관리자가 방장의 페이백 요청을 거절합니다. 정산 상태가 REJECTED로 변경됩니다."
    )
    ResponseEntity<ApiResponse<SettlementResponse>> reject(
            @Parameter(description = "거절할 정산 ID", required = true) @PathVariable Long settlementId
    );
}
