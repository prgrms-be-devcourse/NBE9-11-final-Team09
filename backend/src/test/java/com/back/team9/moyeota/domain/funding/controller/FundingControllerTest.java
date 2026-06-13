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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FundingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FundingService fundingService;

    private final ObjectMapper objectMapper =
            JsonMapper.builder()
                    .findAndAddModules()
                    .build();

    @Test
    void createFunding_정상요청_생성성공() throws Exception {

        FundingCreateRequest request = createOneWayRequest();

        given(
                fundingService.createFunding(
                        anyLong(),
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
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("펀딩 생성 성공"))
                .andExpect(jsonPath("$.data.fundingId").value(1L))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"));
    }

    @Test
    void createFunding_총금액0원_400반환() throws Exception {

        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        0,
                        new RouteRequest(
                                LocalDateTime.of(2027, 6, 20, 8, 0),
                                null,
                                "강남역",
                                Region.SEOUL_A,
                                "잠실역",
                                Region.SEOUL_A
                        )
                );

        mockMvc.perform(
                        post("/api/fundings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createFunding_제목공백_400반환() throws Exception {

        FundingCreateRequest request =
                new FundingCreateRequest(
                        "",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        100000,
                        new RouteRequest(
                                LocalDateTime.of(2027, 6, 20, 8, 0),
                                null,
                                "강남역",
                                Region.SEOUL_A,
                                "잠실역",
                                Region.SEOUL_A
                        )
                );

        mockMvc.perform(
                        post("/api/fundings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updateFunding_정상요청_수정성공() throws Exception {

        FundingUpdateRequest request =
                createUpdateRequest();

        mockMvc.perform(
                        put("/api/fundings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("펀딩 수정 성공"));

        verify(fundingService)
                .updateFunding(
                        eq(1L),
                        eq(1L),
                        any(FundingUpdateRequest.class)
                );
    }

    @Test
    void updateFunding_참가자존재_400반환() throws Exception {

        FundingUpdateRequest request = createUpdateRequest();

        willThrow(
                new BusinessException(
                        ErrorCode.FUNDING_RESTRICTED_UPDATE
                )
        )
                .given(fundingService)
                .updateFunding(
                        eq(1L),
                        eq(1L),
                        any(FundingUpdateRequest.class)
                );

        mockMvc.perform(
                        put("/api/fundings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FND006"))
                .andExpect(
                        jsonPath("$.message")
                                .value("참가자가 존재하는 펀딩은 제목과 내용만 수정할 수 있습니다.")
                );
    }

    @Test
    void cancelFunding_정상요청_취소성공() throws Exception {

        mockMvc.perform(
                        delete("/api/fundings/{id}", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("펀딩 취소 성공"));

        verify(fundingService)
                .cancelFunding(1L,1L);
    }

    @Test
    void cancelFunding_이미취소된펀딩_400반환() throws Exception {

        willThrow(
                new BusinessException(
                        ErrorCode.FUNDING_ALREADY_CANCELLED
                )
        )
                .given(fundingService)
                .cancelFunding(1L,1L);

        mockMvc.perform(
                        delete("/api/fundings/{id}", 1L)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FND004"))
                .andExpect(
                        jsonPath("$.message")
                                .value("이미 취소/삭제된 펀딩입니다.")
                );
    }

    @Test
    void getFunding_정상조회_성공() throws Exception {

        given(
                fundingService.getFunding(1L)
        ).willReturn(
                mock(FundingDetailResponse.class)
        );

        mockMvc.perform(
                        get("/api/fundings/{id}", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("펀딩 조회 성공"));
    }

    @Test
    void getFunding_존재하지않는펀딩_404반환() throws Exception {

        given(
                fundingService.getFunding(999L)
        ).willThrow(
                new BusinessException(
                        ErrorCode.FUNDING_NOT_FOUND
                )
        );

        mockMvc.perform(
                        get("/api/fundings/{id}", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FND001"))
                .andExpect(
                        jsonPath("$.message")
                                .value("존재하지 않는 펀딩입니다.")
                );
    }

    private FundingCreateRequest createOneWayRequest() {
        return new FundingCreateRequest(
                "축구 경기 버스",
                "같이 갑시다",
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                500000,
                new RouteRequest(
                        LocalDateTime.of(2027, 6, 20, 8, 0),
                        null,
                        "인천역",
                        Region.INCHEON,
                        "서울월드컵경기장",
                        Region.SEOUL_A
                )
        );
    }

    private FundingCreateRequest createRoundRequest() {
        return new FundingCreateRequest(
                "축구 경기 버스",
                "같이 갑시다",
                BusType.BUS_45,
                20,
                TripType.ROUND,
                500000,
                new RouteRequest(
                        LocalDateTime.of(2027, 6, 20, 8, 0),
                        LocalDateTime.of(2027, 6, 20, 23, 0),
                        "인천역",
                        Region.INCHEON,
                        "서울월드컵경기장",
                        Region.SEOUL_A
                )
        );
    }

    private FundingUpdateRequest createUpdateRequest() {
        return new FundingUpdateRequest(
                "수정 제목",
                "수정 내용",
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                300000,
                new RouteRequest(
                        LocalDateTime.of(2027, 6, 1, 10, 0),
                        null,
                        "강남역",
                        Region.SEOUL_A,
                        "잠실종합운동장",
                        Region.SEOUL_B
                )
        );
    }
}
