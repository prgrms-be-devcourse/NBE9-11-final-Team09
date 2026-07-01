package com.back.team9.moyeota.domain.payment.client;

import java.math.BigDecimal;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        BigDecimal totalAmount
) {
}
