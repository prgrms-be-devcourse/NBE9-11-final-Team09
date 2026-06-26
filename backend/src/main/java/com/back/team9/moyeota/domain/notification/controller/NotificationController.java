package com.back.team9.moyeota.domain.notification.controller;

import com.back.team9.moyeota.domain.notification.dto.NotificationResponse;
import com.back.team9.moyeota.domain.notification.entity.NotificationType;
import com.back.team9.moyeota.domain.notification.repository.NotificationRepository;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationControllerDocs {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal Long currentMemberId,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        Page<NotificationResponse> response =
                notificationService.getMessages(
                        currentMemberId,
                        pageable
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "SUCCESS",
                        "알림 조회에 성공했습니다.",
                        response
                )
        );
    }
}
