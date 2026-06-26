package com.back.team9.moyeota.domain.settlement.controller;

import com.back.team9.moyeota.domain.settlement.dto.SettlementCreateRequest;
import com.back.team9.moyeota.domain.settlement.dto.SettlementResponse;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.domain.settlement.service.SettlementService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.blacklist.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.resolver.JwtTokenResolver;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class SettlementControllerTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SettlementResponse sampleResponse() {
        return new SettlementResponse(
                1L, new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"),
                SettlementStatus.CALCULATED, false, null, LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("정산 생성 - 정상 요청 201 Created, 수수료 및 상태 응답 검증")
    void create_정상요청_201Created() throws Exception {
        given(settlementService.create(any(SettlementCreateRequest.class), any()))
                .willReturn(sampleResponse());

        mockMvc.perform(post("/api/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SettlementCreateRequest(1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("정산 내역이 생성되었습니다."))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.status").value("CALCULATED"))
                .andExpect(jsonPath("$.data.platformFee").value(10000))
                .andExpect(jsonPath("$.data.hostPaybackAmount").value(90000))
                .andExpect(jsonPath("$.data.paybackPaidAt").doesNotExist());
    }

    @Test
    @DisplayName("정산 생성 - 방장이 아닌 멤버 요청 시 403")
    void create_방장이아닌멤버_403() throws Exception {
        given(settlementService.create(any(SettlementCreateRequest.class), any()))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_ACCESS_DENIED));

        mockMvc.perform(post("/api/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SettlementCreateRequest(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("정산 생성 - 필수 필드(fundingId) 누락 시 400")
    void create_필수필드누락_400() throws Exception {
        mockMvc.perform(post("/api/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정산 조회 - 존재하는 fundingId 요청 시 200 OK")
    void getByFundingId_정상요청_200OK() throws Exception {
        given(settlementService.getByFundingId(eq(1L), any())).willReturn(sampleResponse());

        mockMvc.perform(get("/api/settlements/funding/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("정산 내역을 조회했습니다."))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(100000))
                .andExpect(jsonPath("$.data.status").value("CALCULATED"));
    }

    @Test
    @DisplayName("정산 조회 - 방장이 아닌 멤버 요청 시 403")
    void getByFundingId_방장이아닌멤버_403() throws Exception {
        given(settlementService.getByFundingId(eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_ACCESS_DENIED));

        mockMvc.perform(get("/api/settlements/funding/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("정산 조회 - 존재하지 않는 fundingId 요청 시 404")
    void getByFundingId_존재하지않는정산_404() throws Exception {
        given(settlementService.getByFundingId(eq(999L), any()))
                .willThrow(new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        mockMvc.perform(get("/api/settlements/funding/999"))
                .andExpect(status().isNotFound());
    }
}
