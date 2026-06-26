package com.back.team9.moyeota.domain.settlement.controller;

import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.domain.settlement.service.SettlementService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.blacklist.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.resolver.JwtTokenResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSettlementController.class)
@Import({GlobalExceptionHandler.class, AdminSettlementControllerTest.MethodSecurityTestConfig.class})
@WithMockUser(roles = "ADMIN")
class AdminSettlementControllerTest {

    // @PreAuthorize 메서드 레벨 인가를 @WebMvcTest 슬라이스에서 활성화 (HttpSecurity 불필요)
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementService settlementService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;

    private SettlementResponse approvedResponse() {
        return new SettlementResponse(
                1L, new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"),
                SettlementStatus.APPROVED, false, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private SettlementResponse rejectedResponse() {
        return new SettlementResponse(
                1L, new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"),
                SettlementStatus.REJECTED, false, null, LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("페이백 승인 - 정상 요청 시 200 OK, status APPROVED, paybackPaidAt 존재 확인")
    void approve_정상요청_200OK() throws Exception {
        given(settlementService.approve(1L)).willReturn(approvedResponse());

        mockMvc.perform(patch("/api/admin/settlements/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("페이백 승인이 완료되었습니다."))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.paybackPaidAt").isNotEmpty());
    }

    @Test
    @DisplayName("페이백 승인 - 존재하지 않는 settlementId 요청 시 404")
    void approve_존재하지않는settlementId_404() throws Exception {
        given(settlementService.approve(999L))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/admin/settlements/999/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("페이백 승인 - paybackHold=false인 정산 요청 시 400")
    void approve_paybackHoldFalse인정산_400() throws Exception {
        given(settlementService.approve(1L))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED));

        mockMvc.perform(patch("/api/admin/settlements/1/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("페이백 승인 - CALCULATED 아닌 상태에서 승인 요청 시 400")
    void approve_CALCULATED아닌상태_400() throws Exception {
        given(settlementService.approve(1L))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE));

        mockMvc.perform(patch("/api/admin/settlements/1/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("페이백 거절 - 정상 요청 시 200 OK, status REJECTED, paybackPaidAt null")
    void reject_정상요청_200OK() throws Exception {
        given(settlementService.reject(1L)).willReturn(rejectedResponse());

        mockMvc.perform(patch("/api/admin/settlements/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("페이백 거절이 완료되었습니다."))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.paybackPaidAt").doesNotExist());
    }

    @Test
    @DisplayName("페이백 거절 - 존재하지 않는 settlementId 요청 시 404")
    void reject_존재하지않는settlementId_404() throws Exception {
        given(settlementService.reject(999L))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/admin/settlements/999/reject"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("페이백 거절 - paybackHold=false인 정산 요청 시 400")
    void reject_paybackHoldFalse인정산_400() throws Exception {
        given(settlementService.reject(1L))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_MANUAL_NOT_REQUIRED));

        mockMvc.perform(patch("/api/admin/settlements/1/reject"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("페이백 거절 - CALCULATED 아닌 상태에서 거절 요청 시 400")
    void reject_CALCULATED아닌상태_400() throws Exception {
        given(settlementService.reject(1L))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE));

        mockMvc.perform(patch("/api/admin/settlements/1/reject"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("페이백 승인 - ADMIN 권한 없는 사용자 요청 시 403")
    @WithMockUser(roles = "USER")
    void approve_권한없음_403() throws Exception {
        mockMvc.perform(patch("/api/admin/settlements/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("페이백 거절 - ADMIN 권한 없는 사용자 요청 시 403")
    @WithMockUser(roles = "USER")
    void reject_권한없음_403() throws Exception {
        mockMvc.perform(patch("/api/admin/settlements/1/reject"))
                .andExpect(status().isForbidden());
    }
}
