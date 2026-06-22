package com.back.team9.moyeota.domain.member.controller;

import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.dto.history.MemberFundingResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberParticipationResponse;
import com.back.team9.moyeota.domain.member.dto.history.MemberPaymentResponse;
import com.back.team9.moyeota.domain.member.dto.profile.MemberInfoResponse;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateResponse;
import com.back.team9.moyeota.domain.member.service.auth.MemberLoginService;
import com.back.team9.moyeota.domain.member.service.auth.MemberLogoutService;
import com.back.team9.moyeota.domain.member.service.auth.MemberService;
import com.back.team9.moyeota.domain.member.service.history.MemberHistoryService;
import com.back.team9.moyeota.domain.member.service.profile.MemberProfileService;
import com.back.team9.moyeota.domain.member.service.profile.MemberWithdrawService;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.response.PageResponse;
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
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        MemberAuthController.class,
        MemberProfileController.class,
        MemberHistoryController.class
})
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

    @MockitoBean
    private MemberHistoryService memberHistoryService;

    @MockitoBean
    private MemberWithdrawService memberWithdrawService;

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
    @DisplayName("유효한 이메일 인증 요청은 200 OK를 반환한다")
    void requestEmailVerificationWithValidRequestReturnsOk()
            throws Exception {
        String requestBody = """
                {
                  "email": "moyeota@example.com"
                }
                """;

        mockMvc.perform(post("/api/members/email-verification/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_EMAIL_VERIFICATION_SEND_SUCCESS"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(memberService).requestEmailVerification(any());
    }

    @Test
    @DisplayName("이메일이 누락된 인증 요청은 400 Bad Request를 반환한다")
    void requestEmailVerificationWithoutEmailReturnsBadRequest()
            throws Exception {
        String requestBody = """
                {
                  "email": ""
                }
                """;

        mockMvc.perform(post("/api/members/email-verification/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"));

        verify(memberService, never())
                .requestEmailVerification(any());
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
                                ))))
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
    @DisplayName("인증된 회원이 비밀번호 확인 후 회원 탈퇴한다")
    void withdrawWithValidRequestReturnsOkAndExpiresRefreshTokenCookie()
            throws Exception {
        // Given
        String authorization = "Bearer access-token";
        String requestBody = """
            {
              "password": "Password123!"
            }
            """;

        // When / Then
        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_WITHDRAW_SUCCESS"))
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

        verify(memberWithdrawService)
                .withdraw(any(), any());

        verify(memberLogoutService)
                .logout(authorization);
    }

    @Test
    @DisplayName("회원 탈퇴 시 비밀번호가 누락되면 400 Bad Request를 반환한다")
    void withdrawWithMissingPasswordReturnsBadRequest() throws Exception {
        // Given
        String requestBody = """
            {
              "password": ""
            }
            """;

        // When / Then
        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .with(memberAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"));

        verifyNoInteractions(memberWithdrawService);
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

    @Test
    @DisplayName("인증된 회원의 참여 내역을 페이징 조회한다")
    void getMyParticipationsReturnsPagedParticipationHistory() throws Exception {
        // Given
        MemberParticipationResponse participationResponse =
                new MemberParticipationResponse(
                        1L,
                        10L,
                        "강남 → 부산 합승 모집",
                        LocalDate.of(2026, 7, 10),
                        ParticipationStatus.ACTIVE,
                        ParticipationPaymentStatus.ACTIVE,
                        LocalDateTime.of(2026, 6, 1, 9, 0)
                );

        PageResponse<MemberParticipationResponse> response =
                new PageResponse<>(
                        List.of(participationResponse),
                        0,
                        10,
                        1,
                        1,
                        true,
                        true
                );

        when(memberHistoryService.getMyParticipations(any(), any()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/members/me/participations")
                        .param("page", "0")
                        .param("size", "10")
                        .with(memberAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_GET_MY_PARTICIPATIONS_SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].participationId")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].fundingId")
                        .value(10))
                .andExpect(jsonPath("$.data.content[0].fundingTitle")
                        .value("강남 → 부산 합승 모집"))
                .andExpect(jsonPath("$.data.content[0].departureDate")
                        .value("2026-07-10"))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[0].paymentStatus")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.data.page")
                        .value(0))
                .andExpect(jsonPath("$.data.size")
                        .value(10))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.totalPages")
                        .value(1))
                .andExpect(jsonPath("$.data.first")
                        .value(true))
                .andExpect(jsonPath("$.data.last")
                        .value(true));

        verify(memberHistoryService)
                .getMyParticipations(any(), any());
    }

    @Test
    @DisplayName("인증된 회원의 모집 내역을 페이징 조회한다")
    void getMyFundingsReturnsPagedFundingHistory() throws Exception {
        // Given
        MemberFundingResponse fundingResponse =
                new MemberFundingResponse(
                        10L,
                        "강남 → 부산 합승 모집",
                        LocalDate.of(2026, 7, 10),
                        15L,
                        45,
                        FundingStatus.RECRUITING,
                        LocalDateTime.of(2026, 6, 1, 9, 0)
                );

        PageResponse<MemberFundingResponse> response =
                new PageResponse<>(
                        List.of(fundingResponse),
                        0,
                        10,
                        1,
                        1,
                        true,
                        true
                );

        when(memberHistoryService.getMyFundings(any(), any()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/members/me/fundings")
                        .param("page", "0")
                        .param("size", "10")
                        .with(memberAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_GET_MY_FUNDINGS_SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].fundingId")
                        .value(10))
                .andExpect(jsonPath("$.data.content[0].fundingTitle")
                        .value("강남 → 부산 합승 모집"))
                .andExpect(jsonPath("$.data.content[0].departureDate")
                        .value("2026-07-10"))
                .andExpect(jsonPath("$.data.content[0].currentParticipants")
                        .value(15))
                .andExpect(jsonPath("$.data.content[0].maxParticipants")
                        .value(45))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("RECRUITING"))
                .andExpect(jsonPath("$.data.page")
                        .value(0))
                .andExpect(jsonPath("$.data.size")
                        .value(10))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.totalPages")
                        .value(1))
                .andExpect(jsonPath("$.data.first")
                        .value(true))
                .andExpect(jsonPath("$.data.last")
                        .value(true));

        verify(memberHistoryService)
                .getMyFundings(any(), any());
    }

    @Test
    @DisplayName("인증된 회원의 결제 내역을 페이징 조회한다")
    void getMyPaymentsReturnsPagedPaymentHistory() throws Exception {
        // Given
        MemberPaymentResponse paymentResponse =
                new MemberPaymentResponse(
                        1L,
                        "강남 → 부산 합승 모집",
                        PaymentType.DEPOSIT,
                        new BigDecimal("10000"),
                        PaymentStatus.PAID,
                        LocalDateTime.of(2026, 6, 1, 9, 0)
                );

        PageResponse<MemberPaymentResponse> response =
                new PageResponse<>(
                        List.of(paymentResponse),
                        0,
                        10,
                        1,
                        1,
                        true,
                        true
                );

        when(memberHistoryService.getMyPayments(any(), any()))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/members/me/payments")
                        .param("page", "0")
                        .param("size", "10")
                        .with(memberAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("USR_GET_MY_PAYMENTS_SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].paymentId")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].fundingTitle")
                        .value("강남 → 부산 합승 모집"))
                .andExpect(jsonPath("$.data.content[0].type")
                        .value("DEPOSIT"))
                .andExpect(jsonPath("$.data.content[0].amount")
                        .value(10000))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("PAID"))
                .andExpect(jsonPath("$.data.page")
                        .value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.totalPages")
                        .value(1))
                .andExpect(jsonPath("$.data.first")
                        .value(true))
                .andExpect(jsonPath("$.data.last")
                        .value(true));

        verify(memberHistoryService)
                .getMyPayments(any(), any());
    }
}
