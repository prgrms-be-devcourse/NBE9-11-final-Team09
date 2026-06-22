package com.back.team9.moyeota.domain.admin.controller.management;

import com.back.team9.moyeota.domain.admin.controller.funding.AdminFundingController;
import com.back.team9.moyeota.domain.admin.controller.member.AdminMemberController;
import com.back.team9.moyeota.domain.admin.controller.settlement.AdminSettlementQueryController;
import com.back.team9.moyeota.domain.admin.controller.statistics.AdminStatisticsController;
import com.back.team9.moyeota.domain.admin.dto.funding.*;
import com.back.team9.moyeota.domain.admin.dto.member.*;
import com.back.team9.moyeota.domain.admin.dto.settlement.*;
import com.back.team9.moyeota.domain.admin.dto.statistics.AdminStatisticsResponse;
import com.back.team9.moyeota.domain.admin.service.funding.AdminFundingService;
import com.back.team9.moyeota.domain.admin.service.member.AdminMemberService;
import com.back.team9.moyeota.domain.admin.service.settlement.AdminSettlementQueryService;
import com.back.team9.moyeota.domain.admin.service.statistics.AdminStatisticsService;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.settlement.entity.SettlementStatus;
import com.back.team9.moyeota.global.config.SecurityConfig;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtAuthenticationFilter;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AdminMemberController.class,
        AdminFundingController.class,
        AdminSettlementQueryController.class,
        AdminStatisticsController.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
@DisplayName("관리자 운영 컨트롤러 테스트")
class AdminManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMemberService adminMemberService;

    @MockitoBean
    private AdminFundingService adminFundingService;

    @MockitoBean
    private AdminSettlementQueryService adminSettlementQueryService;

    @MockitoBean
    private AdminStatisticsService adminStatisticsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;

    @Test
    @DisplayName("관리자 회원 목록을 조회한다")
    void getMembersReturnsPagedMembers() throws Exception {
        // Given
        PageResponse<AdminMemberListResponse> response = new PageResponse<>(
                List.of(new AdminMemberListResponse(
                        1L,
                        "member@example.com",
                        "홍길동",
                        "모여타요",
                        "010-1234-5678",
                        null,
                        MemberStatus.ACTIVE,
                        LocalDateTime.of(2026, 6, 1, 10, 0),
                        null
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );

        when(adminMemberService.getMembers(any(Pageable.class)))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/members")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_GET_MEMBERS_SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].memberId")
                        .value(1))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1));

        verify(adminMemberService).getMembers(any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 회원 상세 정보를 조회한다")
    void getMemberReturnsMemberDetail() throws Exception {
        // Given
        AdminMemberDetailResponse response = new AdminMemberDetailResponse(
                1L,
                "member@example.com",
                "홍길동",
                "모여타요",
                "010-1234-5678",
                null,
                null,
                MemberStatus.ACTIVE,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                null,
                3,
                2,
                5
        );

        when(adminMemberService.getMember(1L)).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/members/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_GET_MEMBER_SUCCESS"))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.participationCount")
                        .value(3));

        verify(adminMemberService).getMember(1L);
    }

    @Test
    @DisplayName("관리자 회원 강제 탈퇴를 처리한다")
    void withdrawMemberReturnsWithdrawnMember() throws Exception {
        // Given
        AdminMemberWithdrawResponse response =
                new AdminMemberWithdrawResponse(
                        1L,
                        MemberStatus.WITHDRAWN
                );

        when(adminMemberService.withdrawMember(any(), any()))
                .thenReturn(response);

        String requestBody = """
                {
                  "reason": "운영 정책 위반"
                }
                """;

        // When / Then
        mockMvc.perform(patch("/api/admin/members/1")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_WITHDRAW_MEMBER_SUCCESS"))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        verify(adminMemberService).withdrawMember(any(), any());
    }

    @Test
    @DisplayName("관리자 펀딩 목록을 조회한다")
    void getFundingsReturnsPagedFundings() throws Exception {
        // Given
        PageResponse<AdminFundingListResponse> response = new PageResponse<>(
                List.of(new AdminFundingListResponse(
                        10L,
                        1L,
                        "host@example.com",
                        "잠실 경기 후 인천행 버스",
                        "함께 이동할 참여자를 모집합니다.",
                        LocalDate.of(2026, 7, 10),
                        BusType.BUS_45,
                        FundingStatus.RECRUITING,
                        20,
                        43,
                        18L,
                        LocalDateTime.of(2026, 6, 1, 10, 0)
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );

        when(adminFundingService.getFundings(any(Pageable.class)))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/fundings")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_GET_FUNDINGS_SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].fundingId")
                        .value(10))
                .andExpect(jsonPath("$.data.content[0].currentParticipants")
                        .value(18));
    }

    @Test
    @DisplayName("관리자 정산 목록을 조회한다")
    void getSettlementsReturnsPagedSettlements() throws Exception {
        // Given
        PageResponse<AdminSettlementListResponse> response = new PageResponse<>(
                List.of(new AdminSettlementListResponse(
                        1L,
                        1L,
                        "host@example.com",
                        10L,
                        "잠실 경기 후 인천행 버스",
                        BigDecimal.valueOf(600000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(600000),
                        SettlementStatus.CALCULATED,
                        null,
                        LocalDateTime.of(2026, 6, 20, 23, 0),
                        null
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );

        when(adminSettlementQueryService.getSettlements(any(Pageable.class)))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/settlements")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_GET_SETTLEMENTS_SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].settlementId")
                        .value(1));
    }

    @Test
    @DisplayName("관리자 정산 상세 정보를 조회한다")
    void getSettlementReturnsSettlementDetail() throws Exception {
        // Given
        AdminSettlementDetailResponse response =
                new AdminSettlementDetailResponse(
                        1L,
                        1L,
                        "host@example.com",
                        "버스방장",
                        10L,
                        "잠실 경기 후 인천행 버스",
                        FundingStatus.COMPLETED,
                        LocalDate.of(2026, 7, 10),
                        BigDecimal.valueOf(600000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(600000),
                        SettlementStatus.CALCULATED,
                        null,
                        LocalDateTime.of(2026, 6, 20, 23, 0),
                        null,
                        new AdminSettlementPaymentSummaryResponse(
                                30L,
                                30L,
                                25L,
                                600000L
                        )
                );

        when(adminSettlementQueryService.getSettlement(1L))
                .thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/settlements/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_GET_SETTLEMENT_SUCCESS"))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.paymentSummary.totalPaidCount")
                        .value(30));
    }

    @Test
    @DisplayName("관리자 서비스 통계를 조회한다")
    void getStatisticsReturnsStatistics() throws Exception {
        // Given
        AdminStatisticsResponse response = new AdminStatisticsResponse(
                100,
                90,
                5,
                10,
                20,
                3,
                4820000,
                7,
                0
        );

        when(adminStatisticsService.getStatistics()).thenReturn(response);

        // When / Then
        mockMvc.perform(get("/api/admin/statistics")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_GET_STATISTICS_SUCCESS"))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.totalPaymentAmount")
                        .value(4820000));
    }

    @Test
    @DisplayName("관리자가 펀딩을 강제 취소한다")
    void cancelFundingReturnsCancelledFunding() throws Exception {
        // Given
        AdminFundingCancelResponse response =
                new AdminFundingCancelResponse(
                        10L,
                        FundingStatus.CANCELLED
                );

        when(adminFundingService.cancelFunding(any(), any()))
                .thenReturn(response);

        String requestBody = """
            {
              "reason": "운영 정책 위반으로 인한 강제 취소"
            }
            """;

        // When / Then
        mockMvc.perform(patch("/api/admin/fundings/10")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode")
                        .value("ADMIN_CANCEL_FUNDING_SUCCESS"))
                .andExpect(jsonPath("$.data.fundingId").value(10))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(adminFundingService).cancelFunding(any(), any());
    }

    @Test
    @DisplayName("펀딩 강제 취소 사유가 없으면 400을 반환한다")
    void cancelFundingWithoutReasonReturnsBadRequest() throws Exception {
        // Given
        String requestBody = """
            {
              "reason": ""
            }
            """;

        // When / Then
        mockMvc.perform(patch("/api/admin/fundings/10")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM001"));

        verifyNoInteractions(adminFundingService);
    }

    @Test
    @DisplayName("일반 회원은 관리자 API에 접근할 수 없다")
    void memberCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/statistics")
                        .with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminStatisticsService);
    }
}
