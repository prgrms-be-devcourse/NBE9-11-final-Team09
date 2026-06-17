package com.back.team9.moyeota.domain.seat.controller;

import com.back.team9.moyeota.domain.seat.dto.SeatLayoutResponse;
import com.back.team9.moyeota.domain.seat.dto.SeatResponse;
import com.back.team9.moyeota.domain.seat.entity.SeatDisplayStatus;
import com.back.team9.moyeota.domain.seat.service.SeatService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.exception.GlobalExceptionHandler;
import com.back.team9.moyeota.global.jwt.JwtBlacklistService;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(SeatController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
@DisplayName("좌석 컨트롤러 테스트")
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeatService seatService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenResolver jwtTokenResolver;

    @MockitoBean
    private JwtBlacklistService jwtBlacklistService;


    @Test
    @DisplayName("좌석 배치도 조회 - 정상 요청 200 OK")
    void getSeatLayout_정상요청_200OK() throws Exception {
        // Given: Service가 정상적인 좌석 배치도를 반환하도록 설정
        SeatResponse mockedSeat = new SeatResponse(1L, "1A", SeatDisplayStatus.AVAILABLE, false);
        SeatLayoutResponse response = SeatLayoutResponse.from(1L, "TEMP", List.of(mockedSeat));

        given(seatService.getSeatLayout(eq(1L), anyLong())).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/pathinfos/{pathId}/seats", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data.pathId").value(1L))
                .andExpect(jsonPath("$.data.busType").value("TEMP"))
                .andExpect(jsonPath("$.data.seats[0].seatId").value(1L))
                .andExpect(jsonPath("$.data.seats[0].seatNumber").value("1A"))
                .andExpect(jsonPath("$.data.seats[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.seats[0].mySeat").value(false));

        verify(seatService).getSeatLayout(eq(1L), anyLong());
    }

    @Test
    @DisplayName("좌석 배치도 조회 - 존재하지 않는 노선 404 반환")
    void getSeatLayout_존재하지않는노선_404반환() throws Exception {
        // Given: Service가 PATH_NOT_FOUND 예외를 던지도록 설정
        willThrow(new BusinessException(ErrorCode.PATH_NOT_FOUND))
                .given(seatService)
                .getSeatLayout(eq(999L), anyLong());

        // When & Then
        mockMvc.perform(get("/api/pathinfos/{pathId}/seats", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PTH002"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 노선입니다."));
    }



    @Test
    @DisplayName("좌석 선점 - 정상 요청 200 OK")
    void holdSeat_정상요청_200OK() throws Exception {
        // Given: Service가 정상적으로 HOLD된 좌석 정보를 반환하도록 설정
        SeatResponse response = new SeatResponse(1L, "1A", SeatDisplayStatus.HOLD, true);

        given(seatService.holdSeat(eq(1L), anyLong())).willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/seats/{seatId}/hold", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data.seatId").value(1L))
                .andExpect(jsonPath("$.data.status").value("HOLD"))
                .andExpect(jsonPath("$.data.mySeat").value(true));

        verify(seatService).holdSeat(eq(1L), anyLong());
    }

    @Test
    @DisplayName("좌석 선점 - 존재하지 않는 좌석 404 반환")
    void holdSeat_존재하지않는좌석_404반환() throws Exception {
        // Given: Service가 SEAT_NOT_FOUND 예외를 던지도록 설정
        willThrow(new BusinessException(ErrorCode.SEAT_NOT_FOUND))
                .given(seatService)
                .holdSeat(eq(999L), anyLong());

        // When & Then
        mockMvc.perform(post("/api/seats/{seatId}/hold", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEA001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 좌석입니다."));
    }

    @Test
    @DisplayName("좌석 선점 - 이미 선점된 좌석 409 반환")
    void holdSeat_이미선점된좌석_409반환() throws Exception {
        // Given: Service가 SEAT_ALREADY_OCCUPIED 예외를 던지도록 설정
        willThrow(new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED))
                .given(seatService)
                .holdSeat(eq(1L), anyLong());

        // When & Then
        mockMvc.perform(post("/api/seats/{seatId}/hold", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEA002"))
                .andExpect(jsonPath("$.message").value("이미 선택된 좌석입니다. 새로고침 후 다시 시도해주세요."));
    }

    @Test
    @DisplayName("좌석 선점 - 운행이 마감되거나 취소된 노선 400 반환")
    void holdSeat_마감된노선_400반환() throws Exception {
        // Given: Service가 PATH_INVALID_STATUS 예외를 던지도록 설정
        willThrow(new BusinessException(ErrorCode.PATH_INVALID_STATUS))
                .given(seatService)
                .holdSeat(eq(1L), anyLong());

        // When & Then
        mockMvc.perform(post("/api/seats/{seatId}/hold", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PTH003"))
                .andExpect(jsonPath("$.message").value("운행이 마감되거나 취소된 노선입니다."));
    }
}