package com.back.team9.moyeota.domain.admin.controller.settlement;

import com.back.team9.moyeota.domain.admin.dto.settlement.AdminSettlementDetailResponse;
import com.back.team9.moyeota.domain.admin.dto.settlement.AdminSettlementListResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Admin Settlement Query", description = "관리자 정산 내역 조회 API")
public interface AdminSettlementQueryControllerDocs {

    @Operation(
            summary = "정산 내역 목록 조회",
            description = "전체 정산 내역을 페이지 단위로 조회합니다. PENDING·APPROVED·REJECTED 상태를 모두 포함합니다. 기본 정렬: 생성일 내림차순, 페이지당 20건."
    )
    ResponseEntity<ApiResponse<PageResponse<AdminSettlementListResponse>>> getSettlements(
            Pageable pageable
    );

    @Operation(
            summary = "정산 상세 조회",
            description = "정산 ID로 특정 정산 건의 상세 정보(총액, 수수료, 페이백 금액 등)를 조회합니다."
    )
    ResponseEntity<ApiResponse<AdminSettlementDetailResponse>> getSettlement(
            @Parameter(description = "조회할 정산 ID", required = true) @PathVariable Long settlementId
    );
}
