package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.member.dto.MemberLoginResult;
import com.back.team9.moyeota.domain.member.service.MemberLogoutService;
import com.back.team9.moyeota.domain.member.service.MemberProfileService;
import com.back.team9.moyeota.domain.member.service.MemberService;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.domain.member.dto.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.service.MemberLoginService;
import com.back.team9.moyeota.domain.member.dto.MemberInfoResponse;
import com.back.team9.moyeota.domain.member.dto.MemberUpdateResponse;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;

    @MockitoBean
    private MemberLogoutService memberLogoutService;

    @MockitoBean
    private MemberProfileService memberProfileService;

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
        mockMvc.perform(post("/api/members/signup")
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
        mockMvc.perform(post("/api/members/signup")
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
        mockMvc.perform(post("/api/members/email-verification/confirm")
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
                "Bearer",
                3600,
                new MemberLoginResponse.UserResponse(
                        1L,
                        "moyeota@example.com",
                        "홍길동",
                        "모여타요"
                )
        );

        MemberLoginResult result = new MemberLoginResult(
                response,
                "refresh-token",
                1209600
        );

        when(memberLoginService.login(any())).thenReturn(result);

        String requestBody = """
            {
              "email": "moyeota@example.com",
              "password": "Password123!"
            }
            """;

        // When / Then
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(
                                        "refreshToken=refresh-token"
                                ),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString(
                                        "SameSite=Strict"
                                ),
                                org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("Secure")
                                        )                        )                ))
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
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"));

        verifyNoInteractions(memberLoginService);
    }

    @Test
    @DisplayName("로그아웃 성공 시 Refresh Token 쿠키를 만료한다")
    void logoutExpiresRefreshTokenCookie() throws Exception {
        // Given
        String authorization = "Bearer access-token";

        // When / Then
        mockMvc.perform(post("/api/members/logout")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .with(user("member")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_LOGOUT_SUCCESS"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(
                                        "refreshToken="
                                ),
                                org.hamcrest.Matchers.containsString(
                                        "Max-Age=0"
                                ),
                                org.hamcrest.Matchers.containsString(
                                        "HttpOnly"
                                ),
                                org.hamcrest.Matchers.containsString(
                                        "SameSite=Strict"
                                )
                        )
                ));

        verify(memberLogoutService).logout(authorization);
    }

    @Test
    @DisplayName("인증된 회원의 내 정보를 조회한다")
    void getMyInfoReturnsMemberInfo() throws Exception {
        // Given
        MemberInfoResponse response = new MemberInfoResponse(
                1L,
                "member@example.com",
                "홍길동",
                "모여타요",
                "010-1234-5678",
                null,
                MemberStatus.ACTIVE,
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );

        when(memberProfileService.getMyInfo(any()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/members/me")
                        .with(memberAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_GET_MY_INFO_SUCCESS"))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.email")
                        .value("member@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("모여타요"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(memberProfileService).getMyInfo(any());
    }

    @Test
    @DisplayName("인증된 회원의 닉네임과 전화번호를 수정한다")
    void updateMyInfoReturnsUpdatedMemberInfo() throws Exception {
        // Given
        MemberUpdateResponse response = new MemberUpdateResponse(
                1L,
                "member@example.com",
                "홍길동",
                "변경닉네임",
                "010-9999-8888",
                LocalDateTime.of(2026, 6, 15, 10, 0)
        );

        when(memberProfileService.updateMyInfo(any(), any()))
                .thenReturn(response);

        String requestBody = """
            {
              "nickname": "변경닉네임",
              "phoneNumber": "010-9999-8888"
            }
            """;

        // When / Then
        mockMvc.perform(patch("/api/members/me")
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_UPDATE_MY_INFO_SUCCESS"))
                .andExpect(jsonPath("$.data.nickname")
                        .value("변경닉네임"))
                .andExpect(jsonPath("$.data.phoneNumber")
                        .value("010-9999-8888"));

        verify(memberProfileService).updateMyInfo(any(), any());
    }

    @Test
    @DisplayName("전화번호 형식이 잘못되면 내 정보 수정에 실패한다")
    void updateMyInfoWithInvalidPhoneNumberReturnsBadRequest()
            throws Exception {
        // Given
        String requestBody = """
            {
              "phoneNumber": "01012345678"
            }
            """;

        // When / Then
        mockMvc.perform(patch("/api/members/me")
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"));

        verifyNoInteractions(memberProfileService);
    }

    private RequestPostProcessor memberAuthentication() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of()
                )
        );
    }
}
