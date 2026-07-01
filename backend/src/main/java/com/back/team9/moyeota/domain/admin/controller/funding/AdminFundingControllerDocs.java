package com.back.team9.moyeota.domain.admin.controller.funding;

import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingCancelRequest;
import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingCancelResponse;
import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingListResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import com.back.team9.moyeota.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Funding", description = "관리자 펀딩 조회·강제 취소 API")
public interface AdminFundingControllerDocs {

    @Operation(
            summary = "펀딩 목록 조회",
            description = "전체 펀딩 목록을 페이지 단위로 조회합니다. 기본 정렬: 생성일 내림차순, 페이지당 20건."
    )
    ResponseEntity<ApiResponse<PageResponse<AdminFundingListResponse>>> getFundings(
            Pageable pageable
    );

    @Operation(
            summary = "펀딩 강제 취소",
            description = "관리자가 특정 펀딩을 강제 취소합니다. 취소 사유를 입력해야 하며, 참여자 전원의 결제가 자동 환불됩니다."
    )
    ResponseEntity<ApiResponse<AdminFundingCancelResponse>> cancelFunding(
            @Parameter(description = "취소할 펀딩 ID", required = true) @PathVariable Long fundingId,
            @Valid @RequestBody AdminFundingCancelRequest request
    );
}
