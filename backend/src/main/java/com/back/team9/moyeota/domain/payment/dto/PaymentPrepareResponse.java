package com.back.team9.moyeota.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentPrepareResponse(String orderId, BigDecimal amount) {
}
