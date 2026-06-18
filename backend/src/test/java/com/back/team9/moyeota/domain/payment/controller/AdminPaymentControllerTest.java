package com.back.team9.moyeota.domain.payment.controller;

import com.back.team9.moyeota.domain.payment.service.PaymentService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPaymentController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class AdminPaymentControllerTest {

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

    @Test
    @DisplayName("환불 재시도 - 정상 요청 200 OK")
    void retryRefund_정상요청_200OK() throws Exception {
        willDoNothing().given(paymentService).refundByParticipationId(1L);

        mockMvc.perform(post("/api/admin/payments/retry-refund/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("환불 재처리가 완료되었습니다."));
    }

    @Test
    @DisplayName("환불 재시도 - 존재하지 않는 participationId 요청 시 404")
    void retryRefund_존재하지않는participationId_404() throws Exception {
        willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND))
                .given(paymentService).refundByParticipationId(999L);

        mockMvc.perform(post("/api/admin/payments/retry-refund/999"))
                .andExpect(status().isNotFound());
    }
}
