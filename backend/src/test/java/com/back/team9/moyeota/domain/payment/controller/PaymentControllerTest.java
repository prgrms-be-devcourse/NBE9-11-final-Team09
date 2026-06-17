package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentRefundRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.payment.service.PaymentService;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentResponse sampleResponse(PaymentType type, PaymentStatus status) {
        return new PaymentResponse(
                1L, null, type, "test_orderId",
                50000, "test_paymentKey", status, LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("보증금 결제 승인 - 정상 요청 200 OK")
    void confirmDeposit_정상요청_200OK() throws Exception {
        given(paymentService.confirmDeposit(any(PaymentConfirmRequest.class)))
                .willReturn(sampleResponse(PaymentType.DEPOSIT, PaymentStatus.PAID));

        mockMvc.perform(post("/api/payments/deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PaymentConfirmRequest("test_paymentKey", "test_orderId", 50000, 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("보증금 결제가 완료되었습니다."))
                .andExpect(jsonPath("$.data.paymentType").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("보증금 결제 승인 - 필수 필드 누락 시 400")
    void confirmDeposit_필수필드누락_400() throws Exception {
        mockMvc.perform(post("/api/payments/deposit/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잔액 결제 승인 - 정상 요청 200 OK")
    void confirmBalance_정상요청_200OK() throws Exception {
        given(paymentService.confirmBalance(any(PaymentConfirmRequest.class)))
                .willReturn(sampleResponse(PaymentType.BALANCE, PaymentStatus.PAID));

        mockMvc.perform(post("/api/payments/balance/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PaymentConfirmRequest("test_paymentKey", "test_orderId", 50000, 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("잔액 결제가 완료되었습니다."))
                .andExpect(jsonPath("$.data.paymentType").value("BALANCE"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("잔액 결제 승인 - 필수 필드 누락 시 400")
    void confirmBalance_필수필드누락_400() throws Exception {
        mockMvc.perform(post("/api/payments/balance/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("환불 - 정상 요청 200 OK")
    void refund_정상요청_200OK() throws Exception {
        given(paymentService.refund(eq(1L), any(PaymentRefundRequest.class)))
                .willReturn(sampleResponse(PaymentType.DEPOSIT, PaymentStatus.REFUNDED));

        mockMvc.perform(post("/api/payments/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PaymentRefundRequest("변심"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("환불이 완료되었습니다."))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("환불 - cancelReason 누락 시 400")
    void refund_cancelReason누락_400() throws Exception {
        mockMvc.perform(post("/api/payments/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
