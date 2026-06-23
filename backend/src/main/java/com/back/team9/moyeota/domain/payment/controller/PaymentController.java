package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentPrepareResponse;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.service.PaymentService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/deposit/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmDeposit(
            @RequestBody @Valid PaymentConfirmRequest request
            ) {
        PaymentResponse response = paymentService.confirmDeposit(request);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","보증금 결제가 완료되었습니다.", response));
    }

    @PostMapping("/balance/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmBalance(
            @RequestBody @Valid PaymentConfirmRequest request
    ) {
        PaymentResponse response = paymentService.confirmBalance(request);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","잔액 결제가 완료되었습니다.", response));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable Long paymentId,
            @RequestBody @Valid PaymentRefundRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        PaymentResponse response = paymentService.refund(paymentId, request, memberId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS","환불이 완료되었습니다.", response));
    }

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepare(
            @RequestParam Long participationId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal Long memberId
    ) {
        PaymentPrepareResponse response = paymentService.prepare(participationId, amount, memberId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "결제 준비가 완료되었습니다.", response));
    }
}
