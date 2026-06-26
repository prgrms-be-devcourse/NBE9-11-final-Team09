package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentPrepareResponse;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payment", description = "결제 관련 API")
public interface PaymentControllerDocs {

    @Operation(
            summary = "결제 준비",
            description = "orderId와 결제 금액을 서버에서 발급합니다. Toss Payments 결제창 호출 전 반드시 먼저 호출해야 합니다."
    )
    ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepare(
            @Parameter(description = "결제할 참여 ID", required = true) @RequestParam Long participationId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );

    @Operation(
            summary = "보증금 결제 승인",
            description = "Toss Payments 보증금(DEPOSIT) 결제를 승인합니다. 승인 완료 후 좌석이 확정되고 참여 상태가 DEPOSIT_PAID로 변경됩니다."
    )
    ResponseEntity<ApiResponse<PaymentResponse>> confirmDeposit(
            @RequestBody @Valid PaymentConfirmRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );

    @Operation(
            summary = "잔액 결제 승인",
            description = "Toss Payments 잔액(BALANCE) 결제를 승인합니다. 보증금 결제 완료 후 출발 7일 전에 청구되는 나머지 금액을 처리합니다."
    )
    ResponseEntity<ApiResponse<PaymentResponse>> confirmBalance(
            @RequestBody @Valid PaymentConfirmRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );

    @Operation(
            summary = "결제 환불",
            description = "완료된 결제를 환불합니다. Toss Payments 취소 API를 호출하며, 취소 실패 시 트랜잭션이 롤백되어 결제 상태가 PAID로 유지됩니다."
    )
    ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @Parameter(description = "환불할 결제 ID", required = true) @PathVariable Long paymentId,
            @RequestBody @Valid PaymentRefundRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId
    );
}
