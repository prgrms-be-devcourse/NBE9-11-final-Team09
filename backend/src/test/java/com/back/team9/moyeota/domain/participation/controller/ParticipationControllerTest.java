package com.back.team9.moyeota.domain.participation.controller;

import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.service.ParticipationService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipationController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class ParticipationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipationService participationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 참여 신청 ====================

    @Test
    @DisplayName("참여 신청 - 정상 요청 200 OK")
    void createParticipation_정상요청_200OK() throws Exception {
        ParticipationResponse response = new ParticipationResponse(
                1L,
                ParticipationStatus.ACTIVE,
                ParticipationPaymentStatus.ACTIVE,
                0,
                100L,
                null,
                LocalDateTime.now()
        );

        given(participationService.createParticipation(anyLong(), any(ParticipationCreateRequest.class)))
                .willReturn(response);

        ParticipationCreateRequest request = new ParticipationCreateRequest(10L, 100L, null);

        mockMvc.perform(post("/api/participations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("참여 신청이 완료되었습니다."))
                .andExpect(jsonPath("$.data.participationId").value(1L))
                .andExpect(jsonPath("$.data.outboundSeatId").value(100L));
    }

    @Test
    @DisplayName("참여 신청 - fundingId 누락 시 400")
    void createParticipation_fundingId누락_400반환() throws Exception {
        String invalidRequest = """
                {
                    "outboundSeatId": 100
                }
                """;

        mockMvc.perform(post("/api/participations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    // ==================== 참여 취소 ====================

    @Test
    @DisplayName("참여 취소 - 정상 요청 200 OK")
    void cancelParticipation_정상요청_200OK() throws Exception {
        mockMvc.perform(delete("/api/participations/{participationId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("참여 취소가 완료되었습니다."));
    }

    @Test
    @DisplayName("참여 취소 - 존재하지 않는 참여 404")
    void cancelParticipation_존재하지않는참여_404반환() throws Exception {
        willThrow(new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND))
                .given(participationService)
                .cancelParticipation(anyLong(), eq(999L));

        mockMvc.perform(delete("/api/participations/{participationId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PTC001"))
                .andExpect(jsonPath("$.message").value("탑승 참여 내역을 찾을 수 없습니다."));
    }

    // ==================== 참여자 목록 조회 ====================

    @Test
    @DisplayName("참여자 목록 조회 - 정상 요청 200 OK")
    void getParticipations_정상요청_200OK() throws Exception {
        ParticipationListResponse listResponse = new ParticipationListResponse(
                1L,
                "모여타요",
                ParticipationStatus.ACTIVE,
                ParticipationPaymentStatus.ACTIVE,
                "1A",
                null
        );

        given(participationService.getParticipations(anyLong(), eq(10L)))
                .willReturn(List.of(listResponse));

        mockMvc.perform(get("/api/fundings/{fundingId}/participations", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("참여자 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data[0].memberNickname").value("모여타요"))
                .andExpect(jsonPath("$.data[0].outboundSeatNumber").value("1A"));
    }

    @Test
    @DisplayName("참여자 목록 조회 - 방장 아님 403")
    void getParticipations_방장아님_403반환() throws Exception {
        willThrow(new BusinessException(ErrorCode.FUNDING_FORBIDDEN))
                .given(participationService)
                .getParticipations(anyLong(), eq(10L));

        mockMvc.perform(get("/api/fundings/{fundingId}/participations", 10L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FND007"))
                .andExpect(jsonPath("$.message").value("해당 펀딩에 대한 권한이 없습니다."));
    }
}
