package com.back.team9.moyeota.domain.admin.controller;

import com.back.team9.moyeota.domain.admin.dto.AdminStatisticsResponse;
import com.back.team9.moyeota.domain.admin.service.AdminStatisticsService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(new ApiResponse<>(
                "ADMIN_GET_STATISTICS_SUCCESS",
                "서비스 통계 조회에 성공했습니다.",
                adminStatisticsService.getStatistics()
        ));
    }
}