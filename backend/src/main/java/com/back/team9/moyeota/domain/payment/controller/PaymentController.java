package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.service.PaymentService;
import com.back.team9.moyeota.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
