package com.back.team9.moyeota.domain.admin.controller;

import com.back.team9.moyeota.domain.admin.dto.AdminLoginResponse;
import com.back.team9.moyeota.domain.admin.service.AdminLoginService;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("관리자 인증 컨트롤러 테스트")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminLoginService adminLoginService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;

    @Test
    @DisplayName("올바른 관리자 로그인 요청은 Access Token을 반환한다")
    void loginWithValidRequestReturnsAccessToken() throws Exception {
        // Given
        AdminLoginResponse response = new AdminLoginResponse(
                "admin-access-token",
                "Bearer",
                3600,
                new AdminLoginResponse.AdminResponse(
                        1L,
                        "admin",
                        "SUPER_ADMIN"
                )
        );

        when(adminLoginService.login(any())).thenReturn(response);

        String requestBody = """
                {
                  "loginId": "admin",
                  "password": "Password123!"
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.msg")
                        .value("관리자 로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("admin-access-token"))
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.data.admin.adminId")
                        .value(1))
                .andExpect(jsonPath("$.data.admin.role")
                        .value("SUPER_ADMIN"));

        verify(adminLoginService).login(any());
    }

    @Test
    @DisplayName("관리자 로그인 필수값이 누락되면 400을 반환한다")
    void loginWithMissingFieldsReturnsBadRequest() throws Exception {
        // Given
        String requestBody = """
                {
                  "loginId": "",
                  "password": ""
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COM001"));

        verifyNoInteractions(adminLoginService);
    }
}