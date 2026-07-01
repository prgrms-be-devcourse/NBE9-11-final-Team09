package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.domain.payment.service.PaymentService;
import com.back.team9.moyeota.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController implements AdminPaymentControllerDocs {

    private final PaymentService paymentService;

    @PostMapping("/retry-refund/{participationId}")
    public ResponseEntity<ApiResponse<Void>> retryRefund(@PathVariable Long participationId) {
        paymentService.refundByParticipationId(participationId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "환불 재처리가 완료되었습니다.", null));
    }
}