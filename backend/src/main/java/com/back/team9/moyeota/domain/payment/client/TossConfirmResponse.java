package com.back.team9.moyeota.domain.payment.client;

public record TossConfirmResponse(
        String orderId,
        String paymentKey,
        String amount,
        Integer totalAmount
) {
}
