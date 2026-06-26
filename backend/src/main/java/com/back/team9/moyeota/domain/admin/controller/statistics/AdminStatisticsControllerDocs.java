package com.back.team9.moyeota.domain.admin.controller.statistics;

import com.back.team9.moyeota.domain.admin.dto.statistics.AdminStatisticsResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Statistics", description = "관리자 서비스 현황 통계 API")
public interface AdminStatisticsControllerDocs {

    @Operation(
            summary = "서비스 통계 조회",
            description = "전체 회원 수, 펀딩 수, 총 결제 금액 등 서비스 운영 현황 통계를 조회합니다."
    )
    ResponseEntity<ApiResponse<AdminStatisticsResponse>> getStatistics();
}
