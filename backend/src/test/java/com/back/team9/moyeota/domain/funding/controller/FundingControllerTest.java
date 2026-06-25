package com.back.team9.moyeota.domain.funding.controller;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.service.FundingService;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;
import com.back.team9.moyeota.global.response.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FundingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class FundingControllerTest {

    private static final LocalDateTime DEFAULT_DEPARTURE_TIME =
            LocalDateTime.of(2027, 6, 20, 8, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FundingService fundingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;

    private final ObjectMapper objectMapper =
            JsonMapper.builder()
                    .findAndAddModules()
                    .build();

    @Test
    @DisplayName("펀딩 생성 요청 성공")
    void createFunding_withValidRequest_returnsOk() throws Exception {
        given(
                fundingService.createFunding(
                        any(),
                        any(FundingCreateRequest.class)
                )
        ).willReturn(
                new FundingCreateResponse(
                        1L,
                        FundingStatus.RECRUITING,
                        LocalDateTime.now()
                )
        );

        mockMvc.perform(
                        post("/api/fundings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(oneWayCreateRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data.fundingId").value(1L))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"));

        verify(fundingService)
                .createFunding(
                        any(),
                        any(FundingCreateRequest.class)
                );
    }

    @Test
    @DisplayName("펀딩 생성 빈 제목 400 반환")
    void createFunding_whenTitleIsBlank_returnsBadRequest() throws Exception {
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "",
                        "content",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        "1A",
                        null,
                        oneWayRoute()
                );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 최소인원 0명 400 반환")
    void createFunding_whenMinParticipantsIsZero_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                0,
                TripType.ONE_WAY,
                oneWayRoute()
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 버스타입 미입력 400 반환")
    void createFunding_whenBusTypeIsNull_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                null,
                20,
                TripType.ONE_WAY,
                oneWayRoute()
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 왕복여부 미입력 400 반환")
    void createFunding_whenTripTypeIsNull_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                null,
                oneWayRoute()
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 방장 출발 좌석 미입력 400 반환")
    void createFunding_whenHostOutboundSeatNumberIsBlank_returnsBadRequest() throws Exception {
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "Football Match Bus",
                        "Ride together",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        "",
                        null,
                        oneWayRoute()
                );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 노선 미입력 400 반환")
    void createFunding_whenRouteIsNull_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                null
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 출발시간 미입력 400 반환")
    void createFunding_whenRouteDepartureTimeIsNull_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                new RouteRequest(
                        null,
                        null,
                        "Incheon Terminal",
                        Region.INCHEON,
                        "Seoul Stadium",
                        Region.SEOUL
                )
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 출발지 미입력 400 반환")
    void createFunding_whenRouteDepartureAddressIsBlank_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                new RouteRequest(
                        DEFAULT_DEPARTURE_TIME,
                        null,
                        "",
                        Region.INCHEON,
                        "Seoul Stadium",
                        Region.SEOUL
                )
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 생성 도착구역 미입력 400 반환")
    void createFunding_whenRouteArrivalRegionIsNull_returnsBadRequest() throws Exception {
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                new RouteRequest(
                        DEFAULT_DEPARTURE_TIME,
                        null,
                        "Incheon Terminal",
                        Region.INCHEON,
                        "Seoul Stadium",
                        null
                )
        );

        performCreateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 수정 요청 성공")
    void updateFunding_withValidRequest_returnsOk() throws Exception {
        mockMvc.perform(
                        patch("/api/fundings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(oneWayUpdateRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").exists());

        verify(fundingService)
                .updateFunding(
                        any(),
                        eq(1L),
                        any(FundingUpdateRequest.class)
                );
    }

    @Test
    @DisplayName("펀딩 수정 참가자 존재 400 반환")
    void updateFunding_whenServiceThrowsRestrictedUpdate_returnsFnd006() throws Exception {
        willThrow(new BusinessException(ErrorCode.FUNDING_UPDATE_RESTRICTED_BY_PARTICIPANTS))
                .given(fundingService)
                .updateFunding(
                        any(),
                        eq(1L),
                        any(FundingUpdateRequest.class)
                );

        mockMvc.perform(
                        patch("/api/fundings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(oneWayUpdateRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FND005"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("펀딩 수정 제목 미입력 400 반환")
    void updateFunding_whenTitleIsBlank_returnsBadRequest() throws Exception {
        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "",
                        "content",
                        BusType.BUS_25,
                        10,
                        TripType.ONE_WAY,
                        "1A",
                        null,
                        oneWayRoute()
                );

        performUpdateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 수정 노선 미입력 400 반환")
    void updateFunding_whenRouteIsNull_returnsBadRequest() throws Exception {
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                null
        );

        performUpdateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 수정 도착지 미입력 400 반환")
    void updateFunding_whenRouteArrivalAddressIsBlank_returnsBadRequest() throws Exception {
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                new RouteRequest(
                        DEFAULT_DEPARTURE_TIME,
                        null,
                        "Incheon Terminal",
                        Region.INCHEON,
                        "",
                        Region.SEOUL
                )
        );

        performUpdateBadRequest(request);
    }

    @Test
    @DisplayName("펀딩 취소 요청 성공")
    void cancelFunding_withValidRequest_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/fundings/{id}", 1L)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").exists());

        verify(fundingService).cancelFunding(any(), eq(1L));
    }

    @Test
    @DisplayName("펀딩 취소 이미 취소된 펀딩 400 반환")
    void cancelFunding_whenServiceThrowsAlreadyCancelled_returnsFnd004() throws Exception {
        willThrow(new BusinessException(ErrorCode.FUNDING_ALREADY_CANCELLED))
                .given(fundingService)
                .cancelFunding(any(), eq(1L));

        mockMvc.perform(delete("/api/fundings/{id}", 1L)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FND008"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("펀딩 상세 조회 성공")
    void getFunding_withExistingFunding_returnsOk() throws Exception {
        given(fundingService.getFunding(eq(1L), nullable(Long.class)))
                .willReturn(mock(FundingDetailResponse.class));

        mockMvc.perform(get("/api/fundings/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").exists());

        verify(fundingService).getFunding(eq(1L), nullable(Long.class));
    }

    @Test
    @DisplayName("펀딩 상세 조회 존재하지 않는 펀딩 404 반환")
    void getFunding_whenServiceThrowsFundingNotFound_returnsFnd001() throws Exception {
        given(fundingService.getFunding(eq(999L), nullable(Long.class)))
                .willThrow(new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        mockMvc.perform(get("/api/fundings/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FND001"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("펀딩 목록 조회 필터링 성공")
    void getFundingList_withFilters_returnsPageResponse() throws Exception {
        given(
                fundingService.getFundingList(
                        any(FundingSearchCondition.class),
                        any(Pageable.class)
                )
        ).willReturn(fundingListPageResponse());

        mockMvc.perform(
                        get("/api/fundings")
                                .param("statuses", "RECRUITING")
                                .param("departureRegion", "INCHEON")
                                .param("arrivalRegion", "SEOUL")
                                .param("departureDate", "2027-06-20")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].fundingId").value(1L))
                .andExpect(jsonPath("$.data.content[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.content[0].tripType").value("ONE_WAY"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(fundingService)
                .getFundingList(
                        any(FundingSearchCondition.class),
                        any(Pageable.class)
                );
    }

    private void performCreateBadRequest(FundingCreateRequest request) throws Exception {
        mockMvc.perform(
                        post("/api/fundings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COM001"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(fundingService);
    }

    private void performUpdateBadRequest(FundingUpdateRequest request) throws Exception {
        mockMvc.perform(
                        patch("/api/fundings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COM001"))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(fundingService);
    }

    private FundingCreateRequest oneWayCreateRequest() {
        return createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                oneWayRoute()
        );
    }

    private FundingCreateRequest createRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            RouteRequest route
    ) {
        return new FundingCreateRequest(
                "Football Match Bus",
                "Ride together",
                busType,
                minParticipants,
                tripType,
                "1A",
                tripType == TripType.ROUND ? "1B" : null,
                route
        );
    }

    private FundingUpdateRequest oneWayUpdateRequest() {
        return updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                oneWayRoute()
        );
    }

    private FundingUpdateRequest updateRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            RouteRequest route
    ) {
        return new FundingUpdateRequest(
                "Updated Title",
                "Updated Content",
                busType,
                minParticipants,
                tripType,
                "1A",
                tripType == TripType.ROUND ? "1B" : null,
                route
        );
    }

    private RouteRequest oneWayRoute() {
        return new RouteRequest(
                DEFAULT_DEPARTURE_TIME,
                null,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL
        );
    }

    private PageResponse<FundingListResponse> fundingListPageResponse() {
        return new PageResponse<>(
                List.of(
                        new FundingListResponse(
                                1L,
                                "title",
                                "host",
                                "departure",
                                "arrival",
                                DEFAULT_DEPARTURE_TIME,
                                FundingStatus.RECRUITING,
                                0,
                                20,
                                45,
                                TripType.ONE_WAY,
                                BigDecimal.valueOf(500000),
                                null,
                                BigDecimal.valueOf(11200),
                                BigDecimal.valueOf(25000)
                        )
                ),
                0,
                20,
                1,
                1,
                true,
                true
        );
    }
}
