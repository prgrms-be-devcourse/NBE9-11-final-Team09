package com.back.team9.moyeota.domain.payment.client;

import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class TossPaymentClient {

    private final RestClient restClient;

    public TossPaymentClient(
            @Value("${toss.secret-key}") String secretKey,
            @Value("${toss.base-url}") String baseUrl
    ) {
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Basic " + encodedKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public TossConfirmResponse confirm(String paymentKey, String orderId, Integer amount) {
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .body(new ConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);
        } catch (Exception e) {
            log.error("Toss payment confirmation failed. paymentKey: {}, orderId: {}", paymentKey, orderId, e);
            throw new BusinessException(ErrorCode.TOSS_PAYMENT_FAILED);
        }
    }

    public void cancel(String paymentKey, String cancelReason) {
        try {
            restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .body(new CancelRequest(cancelReason))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Toss payment cancel failed. paymentKey: {}", paymentKey, e);
            throw new BusinessException(ErrorCode.REFUND_FAILED);
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, Integer amount) {
    }
    private record CancelRequest(String cancelReason) {
    }
}
