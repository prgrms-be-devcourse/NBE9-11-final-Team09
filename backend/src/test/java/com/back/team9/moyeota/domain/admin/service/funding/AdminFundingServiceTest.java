package com.back.team9.moyeota.domain.admin.service.funding;

import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingCancelRequest;
import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingCancelResponse;
import com.back.team9.moyeota.domain.admin.dto.funding.AdminFundingListResponse;
import com.back.team9.moyeota.domain.admin.repository.funding.AdminFundingQueryRepository;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.notification.service.NotificationService;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 펀딩 서비스 테스트")
class AdminFundingServiceTest {

    @Mock
    private AdminFundingQueryRepository fundingRepository;

    @Mock
    private PathinfoService pathinfoService;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

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
                43,
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

    @ParameterizedTest
    @EnumSource(
            value = FundingStatus.class,
            names = {"RECRUITING", "CONFIRMED"}
    )
    @DisplayName("취소 가능한 상태의 펀딩을 강제 취소한다")
    void cancelFundingWithCancellableStatusChangesStatus(
            FundingStatus fundingStatus
    ) {
        // Given
        Funding funding = createFunding(fundingStatus);
        AdminFundingCancelRequest request =
                new AdminFundingCancelRequest("운영 정책 위반");

        when(fundingRepository.findById(10L))
                .thenReturn(Optional.of(funding));
        when(participationRepository.findByFunding_FundingIdAndStatus(10L, ParticipationStatus.ACTIVE))
                .thenReturn(List.of());

        // When
        AdminFundingCancelResponse response =
                adminFundingService.cancelFunding(10L, request);

        // Then
        assertThat(response.fundingId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(FundingStatus.CANCELLED);
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CANCELLED);
        verify(pathinfoService).cancelPathinfos(10L);
    }

    @Test
    @DisplayName("취소 시 ACTIVE 참여자가 있으면 환불 이벤트를 발행하고 알림을 발송한다")
    void cancelFunding_ACTIVE참여자있을때_이벤트발행_알림발송() {
        // Given
        Funding funding = createFunding(FundingStatus.RECRUITING);
        AdminFundingCancelRequest request = new AdminFundingCancelRequest("운영 정책 위반");

        Participation p1 = mock(Participation.class);
        Participation p2 = mock(Participation.class);
        given(p1.getParticipationId()).willReturn(1L);
        given(p2.getParticipationId()).willReturn(2L);

        when(fundingRepository.findById(10L)).thenReturn(Optional.of(funding));
        when(participationRepository.findByFunding_FundingIdAndStatus(10L, ParticipationStatus.ACTIVE))
                .thenReturn(List.of(p1, p2));

        // When
        adminFundingService.cancelFunding(10L, request);

        // Then
        ArgumentCaptor<ParticipationCancelledEvent> captor =
                ArgumentCaptor.forClass(ParticipationCancelledEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ParticipationCancelledEvent::participationId)
                .containsExactlyInAnyOrder(1L, 2L);
        verify(notificationService).sendToFundingParticipants(eq(10L), any());
    }

    @Test
    @DisplayName("이미 취소된 펀딩을 강제 취소하면 예외가 발생한다")
    void cancelAlreadyCancelledFundingThrowsException() {
        // Given
        Funding funding = createFunding(FundingStatus.CANCELLED);
        AdminFundingCancelRequest request =
                new AdminFundingCancelRequest("운영 정책 위반");

        when(fundingRepository.findById(10L))
                .thenReturn(Optional.of(funding));

        // When / Then
        assertBusinessException(
                () -> adminFundingService.cancelFunding(10L, request),
                ErrorCode.ADMIN_FUNDING_ALREADY_CANCELLED
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = FundingStatus.class,
            names = {"COMPLETED", "FAILED"}
    )
    @DisplayName("취소할 수 없는 상태의 펀딩을 강제 취소하면 예외가 발생한다")
    void cancelFundingWithInvalidStatusThrowsException(
            FundingStatus fundingStatus
    ) {
        // Given
        Funding funding = createFunding(fundingStatus);
        AdminFundingCancelRequest request =
                new AdminFundingCancelRequest("운영 정책 위반");

        when(fundingRepository.findById(10L))
                .thenReturn(Optional.of(funding));

        // When / Then
        assertBusinessException(
                () -> adminFundingService.cancelFunding(10L, request),
                ErrorCode.ADMIN_FUNDING_CANCEL_NOT_ALLOWED
        );
    }

    @Test
    @DisplayName("존재하지 않는 펀딩을 강제 취소하면 예외가 발생한다")
    void cancelUnknownFundingThrowsException() {
        // Given
        AdminFundingCancelRequest request =
                new AdminFundingCancelRequest("운영 정책 위반");

        when(fundingRepository.findById(10L))
                .thenReturn(Optional.empty());

        // When / Then
        assertBusinessException(
                () -> adminFundingService.cancelFunding(10L, request),
                ErrorCode.FUNDING_NOT_FOUND
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("강제 취소 사유가 없으면 예외가 발생한다")
    void cancelFundingWithoutReasonThrowsException(String reason) {
        // Given
        AdminFundingCancelRequest request =
                new AdminFundingCancelRequest(reason);

        // When / Then
        assertBusinessException(
                () -> adminFundingService.cancelFunding(10L, request),
                ErrorCode.INVALID_INPUT_VALUE
        );

        verifyNoInteractions(fundingRepository);
    }

    private Funding createFunding(FundingStatus status) {
        return Funding.builder()
                .fundingId(10L)
                .status(status)
                .build();
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }
}
