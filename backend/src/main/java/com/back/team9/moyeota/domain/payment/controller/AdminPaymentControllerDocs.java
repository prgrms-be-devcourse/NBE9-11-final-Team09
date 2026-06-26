package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Admin Payment", description = "관리자 환불 재처리 API")
public interface AdminPaymentControllerDocs {

    @Operation(
            summary = "환불 재처리",
            description = "Toss Payments 환불 실패 건을 관리자가 수동으로 재처리합니다. 해당 참여의 PAID 상태 결제를 찾아 환불을 재시도합니다."
    )
    ResponseEntity<ApiResponse<Void>> retryRefund(
            @Parameter(description = "재처리할 참여 ID", required = true) @PathVariable Long participationId
    );
}
