package com.back.team9.moyeota.domain.payment.client;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount
) {
}
