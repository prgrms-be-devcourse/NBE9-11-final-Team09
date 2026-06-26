package com.back.team9.moyeota.domain.notification.controller;

import com.back.team9.moyeota.domain.notification.dto.NotificationResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Notification", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(
            summary = "알림 목록 조회",
            description = "로그인한 회원의 알림 목록을 페이지 단위로 조회합니다. 펀딩 상태 변경, 결제 완료, 정산 승인 등 다양한 알림 유형이 포함됩니다. 기본 정렬: 최신순, 페이지당 20건."
    )
    ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal Long currentMemberId,
            Pageable pageable
    );
}
