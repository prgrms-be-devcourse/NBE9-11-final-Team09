package com.back.team9.moyeota.domain.admin.service;

import com.back.team9.moyeota.domain.admin.dto.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.repository.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 펀딩 서비스 테스트")
class AdminFundingServiceTest {

    @Mock
    private AdminFundingQueryRepository fundingRepository;

    @InjectMocks
    private AdminFundingService adminFundingService;

    @Test
    @DisplayName("펀딩 목록을 페이징 조회한다")
    void getFundingsReturnsPagedFundings() {
        // Given
        PageRequest pageable = PageRequest.of(0, 20);
        AdminFundingListResponse funding = new AdminFundingListResponse(
                10L,
                1L,
                "host@example.com",
                "잠실 경기 후 인천행 버스",
                "함께 이동할 참여자를 모집합니다.",
                LocalDate.of(2026, 7, 10),
                BusType.BUS_45,
                FundingStatus.RECRUITING,
                20,
                44,
                18L,
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );

        when(fundingRepository.findAdminFundings(
                ParticipationStatus.CANCELED,
                pageable
        )).thenReturn(new PageImpl<>(List.of(funding), pageable, 1));

        // When
        PageResponse<AdminFundingListResponse> response =
                adminFundingService.getFundings(pageable);

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().fundingId()).isEqualTo(10L);
        assertThat(response.content().getFirst().currentParticipants())
                .isEqualTo(18L);
        assertThat(response.totalElements()).isEqualTo(1);

        verify(fundingRepository).findAdminFundings(
                ParticipationStatus.CANCELED,
                pageable
        );
    }
}
