package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.service.MemberService;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.domain.member.dto.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.service.MemberLoginService;
import static org.mockito.Mockito.when;

import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({GlobalExceptionHandler.class})
@DisplayName("회원 컨트롤러 테스트")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberLoginService memberLoginService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("유효한 회원가입 요청 시 201 Created를 반환한다")
    void requestSignupWithValidRequestReturnsCreated() throws Exception {
        // Given
        String requestBody = """
                {
                  "email": "moyeota@example.com",
                  "password": "Password123!",
                  "name": "홍길동",
                  "nickname": "모여타요",
                  "phoneNumber": "010-1234-5678"
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_SIGNUP_REQUEST_SUCCESS"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(memberService).requestSignup(any());
    }

    @Test
    @DisplayName("회원가입 필수 입력값이 누락되면 400 Bad Request를 반환한다")
    void requestSignupWithMissingRequiredFieldsReturnsBadRequest()
            throws Exception {
        // Given
        String requestBody = """
                {
                  "email": "",
                  "password": "",
                  "name": "",
                  "nickname": "",
                  "phoneNumber": ""
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COM001"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(memberService);
    }

    @Test
    @DisplayName("유효한 이메일 인증 확인 요청 시 200 OK를 반환한다")
    void confirmEmailVerificationWithValidRequestReturnsOk()
            throws Exception {
        // Given
        String requestBody = """
                {
                  "email": "moyeota@example.com",
                  "verificationCode": "A1B2C3"
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/users/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_EMAIL_VERIFICATION_SUCCESS"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(memberService).confirmEmailVerification(any());
    }

    @Test
    @DisplayName("유효한 로그인 요청 시 200 OK와 토큰을 반환한다")
    void loginWithValidRequestReturnsOkAndTokens() throws Exception {
        // Given
        MemberLoginResponse response = new MemberLoginResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                3600,
                1209600,
                new MemberLoginResponse.UserResponse(
                        1L,
                        "moyeota@example.com",
                        "홍길동",
                        "모여타요"
                )
        );

        when(memberLoginService.login(any())).thenReturn(response);

        String requestBody = """
            {
              "email": "moyeota@example.com",
              "password": "Password123!"
            }
            """;

        // When / Then
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken")
                        .value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.data.user.userId").value(1));

        verify(memberLoginService).login(any());
    }

    @Test
    @DisplayName("로그인 필수 입력값이 누락되면 400 Bad Request를 반환한다")
    void loginWithMissingRequiredFieldsReturnsBadRequest() throws Exception {
        // Given
        String requestBody = """
            {
              "email": "",
              "password": ""
            }
            """;

        // When / Then
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"));

        verifyNoInteractions(memberLoginService);
    }
}