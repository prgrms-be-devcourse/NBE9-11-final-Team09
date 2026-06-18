package com.back.team9.moyeota.domain.pathinfo.service;

import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.pathinfo.validator.PathinfoValidator;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PathinfoServiceUnitTest {

    private static final LocalDateTime DEPARTURE_TIME =
            LocalDateTime.of(2027, 6, 20, 8, 0);
    private static final LocalDateTime RETURN_TIME =
            LocalDateTime.of(2027, 6, 20, 23, 0);

    @InjectMocks
    private PathinfoService pathinfoService;

    @Mock
    private PathinfoRepository pathinfoRepository;

    @Mock
    private PathinfoValidator pathinfoValidator;

    @Test
    @DisplayName("노선 생성 - 편도면 가는 노선만 저장한다")
    void createPathinfos_whenOneWay_savesOutboundOnly() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.RECRUITING);
        RouteRequest route = oneWayRoute();

        // When
        pathinfoService.createPathinfos(funding, TripType.ONE_WAY, route);

        // Then
        ArgumentCaptor<Pathinfo> captor = ArgumentCaptor.forClass(Pathinfo.class);
        verify(pathinfoValidator).validateTripType(TripType.ONE_WAY, route);
        verify(pathinfoRepository).save(captor.capture());

        Pathinfo saved = captor.getValue();
        assertThat(saved.getFunding()).isEqualTo(funding);
        assertThat(saved.getDirection()).isEqualTo(Direction.OUTBOUND);
        assertThat(saved.getDepartureTime()).isEqualTo(DEPARTURE_TIME);
        assertThat(saved.getBusType()).isEqualTo(BusType.BUS_45);
    }

    @Test
    @DisplayName("노선 생성 - 왕복이면 가는 노선과 오는 노선을 저장한다")
    void createPathinfos_whenRound_savesOutboundAndReturn() {
        // Given
        Funding funding = funding(1L, TripType.ROUND, FundingStatus.RECRUITING);
        RouteRequest route = roundRoute();

        // When
        pathinfoService.createPathinfos(funding, TripType.ROUND, route);

        // Then
        ArgumentCaptor<Pathinfo> captor = ArgumentCaptor.forClass(Pathinfo.class);
        verify(pathinfoValidator).validateTripType(TripType.ROUND, route);
        verify(pathinfoRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());

        List<Pathinfo> saved = captor.getAllValues();
        assertThat(saved)
                .extracting(Pathinfo::getDirection)
                .containsExactly(Direction.OUTBOUND, Direction.RETURN);
        assertThat(saved.get(1).getDepartureTime()).isEqualTo(RETURN_TIME);
        assertThat(saved.get(1).getDepartureAddress()).isEqualTo("Seoul Stadium");
        assertThat(saved.get(1).getArrivalAddress()).isEqualTo("Incheon Terminal");
    }

    @Test
    @DisplayName("노선 생성 - 검증 실패 시 저장하지 않는다")
    void createPathinfos_whenValidationFails_doesNotSave() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.RECRUITING);
        RouteRequest route = oneWayRoute();

        willThrow(new BusinessException(ErrorCode.DEPARTURE_DATE_TOO_SOON))
                .given(pathinfoValidator)
                .validateTripType(TripType.ONE_WAY, route);

        // When / Then
        assertThatThrownBy(() ->
                pathinfoService.createPathinfos(funding, TripType.ONE_WAY, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTURE_DATE_TOO_SOON);

        verify(pathinfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("노선 수정 - 가는 노선이 없으면 예외")
    void updatePathinfos_whenOutboundDoesNotExist_throwsException() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.RECRUITING);
        RouteRequest route = oneWayRoute();

        given(pathinfoRepository.findByFunding_FundingIdAndDirection(
                1L,
                Direction.OUTBOUND
        )).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                pathinfoService.updatePathinfos(funding, TripType.ONE_WAY, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PATHINFO_REQUIRED);

        verify(pathinfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("노선 수정 - 검증 실패 시 조회하지 않는다")
    void updatePathinfos_whenValidationFails_doesNotFindPathinfos() {
        // Given
        Funding funding = funding(1L, TripType.ROUND, FundingStatus.RECRUITING);
        RouteRequest route = roundRoute();

        willThrow(new BusinessException(ErrorCode.INVALID_PATH_CONFIGURATION))
                .given(pathinfoValidator)
                .validateTripType(TripType.ROUND, route);

        // When / Then
        assertThatThrownBy(() ->
                pathinfoService.updatePathinfos(funding, TripType.ROUND, route)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);

        verifyNoInteractions(pathinfoRepository);
    }

    @Test
    @DisplayName("노선 수정 - 편도에서 왕복으로 바꾸면 오는 노선을 생성한다")
    void updatePathinfos_fromOneWayToRound_createsReturnPathinfo() {
        // Given
        Funding funding = funding(1L, TripType.ROUND, FundingStatus.RECRUITING);
        RouteRequest route = roundRoute();
        Pathinfo outbound = outboundPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingIdAndDirection(
                1L,
                Direction.OUTBOUND
        )).willReturn(Optional.of(outbound));
        given(pathinfoRepository.findByFunding_FundingIdAndDirection(
                1L,
                Direction.RETURN
        )).willReturn(Optional.empty());

        // When
        pathinfoService.updatePathinfos(funding, TripType.ROUND, route);

        // Then
        ArgumentCaptor<Pathinfo> captor = ArgumentCaptor.forClass(Pathinfo.class);
        verify(pathinfoRepository).save(captor.capture());

        assertThat(outbound.getDepartureTime()).isEqualTo(DEPARTURE_TIME);
        assertThat(captor.getValue().getDirection()).isEqualTo(Direction.RETURN);
        assertThat(captor.getValue().getStatus()).isEqualTo(PathinfoStatus.PENDING);
    }

    @Test
    @DisplayName("노선 수정 - 왕복에서 편도로 바꾸면 오는 노선을 취소한다")
    void updatePathinfos_fromRoundToOneWay_cancelsReturnPathinfo() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.RECRUITING);
        RouteRequest route = oneWayRoute();
        Pathinfo outbound = outboundPathinfo(funding);
        Pathinfo returned = returnPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingIdAndDirection(
                1L,
                Direction.OUTBOUND
        )).willReturn(Optional.of(outbound));
        given(pathinfoRepository.findByFunding_FundingIdAndDirection(
                1L,
                Direction.RETURN
        )).willReturn(Optional.of(returned));

        // When
        pathinfoService.updatePathinfos(funding, TripType.ONE_WAY, route);

        // Then
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);
        verify(pathinfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("노선 취소 - 조회된 모든 노선을 취소한다")
    void cancelPathinfos_cancelsAllPathinfos() {
        // Given
        Funding funding = funding(1L, TripType.ROUND, FundingStatus.RECRUITING);
        Pathinfo outbound = outboundPathinfo(funding);
        Pathinfo returned = returnPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingId(1L))
                .willReturn(List.of(outbound, returned));

        // When
        pathinfoService.cancelPathinfos(1L);

        // Then
        assertThat(outbound.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);
    }

    @Test
    @DisplayName("버스 타입 동기화 - 조회된 모든 노선의 버스 타입을 변경한다")
    void syncBusType_changesAllPathinfoBusTypes() {
        // Given
        Funding funding = funding(1L, TripType.ROUND, FundingStatus.RECRUITING);
        Pathinfo outbound = outboundPathinfo(funding);
        Pathinfo returned = returnPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingId(1L))
                .willReturn(List.of(outbound, returned));

        // When
        pathinfoService.syncBusType(1L, BusType.BUS_25);

        // Then
        assertThat(outbound.getBusType()).isEqualTo(BusType.BUS_25);
        assertThat(returned.getBusType()).isEqualTo(BusType.BUS_25);
    }

    @Test
    @DisplayName("상세 노선 응답 - 일반 상태면 취소 노선을 제외하고 조회한다")
    void getPathinfoResponsesForDetail_whenActiveFunding_excludesCancelledPathinfos() {
        // Given
        Funding funding = funding(1L, TripType.ROUND, FundingStatus.RECRUITING);
        Pathinfo outbound = outboundPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingIdAndStatusNot(
                1L,
                PathinfoStatus.CANCELLED
        )).willReturn(List.of(outbound));

        // When
        var responses = pathinfoService.getPathinfoResponsesForDetail(funding);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).direction()).isEqualTo(Direction.OUTBOUND);
    }

    @Test
    @DisplayName("상세 노선 응답 - 취소된 편도 펀딩이면 가는 노선만 반환한다")
    void getPathinfoResponsesForDetail_whenCancelledOneWay_returnsOutboundOnly() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.CANCELLED);
        Pathinfo outbound = outboundPathinfo(funding);
        Pathinfo returned = returnPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingId(1L))
                .willReturn(List.of(outbound, returned));

        // When
        var responses = pathinfoService.getPathinfoResponsesForDetail(funding);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).direction()).isEqualTo(Direction.OUTBOUND);
    }

    @Test
    @DisplayName("노선 변경 확인 - 가는 노선이 바뀌면 true를 반환한다")
    void isRouteChanged_whenOutboundChanged_returnsTrue() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.RECRUITING);
        Pathinfo outbound = outboundPathinfo(funding);
        RouteRequest changedRoute = new RouteRequest(
                DEPARTURE_TIME,
                null,
                "Changed Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL
        );

        given(pathinfoRepository.findByFunding_FundingId(1L))
                .willReturn(List.of(outbound));

        // When
        boolean result = pathinfoService.isRouteChanged(
                1L,
                TripType.ONE_WAY,
                changedRoute
        );

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("노선 변경 확인 - 가는 노선이 없으면 예외")
    void isRouteChanged_whenOutboundDoesNotExist_throwsException() {
        // Given
        given(pathinfoRepository.findByFunding_FundingId(1L))
                .willReturn(List.of());

        // When / Then
        assertThatThrownBy(() ->
                pathinfoService.isRouteChanged(
                        1L,
                        TripType.ONE_WAY,
                        oneWayRoute()
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PATHINFO_REQUIRED);
    }

    @Test
    @DisplayName("노선 변경 확인 - 편도 노선이 같으면 false를 반환한다")
    void isRouteChanged_whenOneWayRouteIsSame_returnsFalse() {
        // Given
        Funding funding = funding(1L, TripType.ONE_WAY, FundingStatus.RECRUITING);
        Pathinfo outbound = outboundPathinfo(funding);

        given(pathinfoRepository.findByFunding_FundingId(1L))
                .willReturn(List.of(outbound));

        // When
        boolean result = pathinfoService.isRouteChanged(
                1L,
                TripType.ONE_WAY,
                oneWayRoute()
        );

        // Then
        assertThat(result).isFalse();
    }

    private RouteRequest oneWayRoute() {
        return new RouteRequest(
                DEPARTURE_TIME,
                null,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL
        );
    }

    private RouteRequest roundRoute() {
        return new RouteRequest(
                DEPARTURE_TIME,
                RETURN_TIME,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL
        );
    }

    private Pathinfo outboundPathinfo(Funding funding) {
        return Pathinfo.create(
                funding,
                DEPARTURE_TIME,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL,
                Direction.OUTBOUND
        );
    }

    private Pathinfo returnPathinfo(Funding funding) {
        return Pathinfo.create(
                funding,
                RETURN_TIME,
                "Seoul Stadium",
                Region.SEOUL,
                "Incheon Terminal",
                Region.INCHEON,
                Direction.RETURN
        );
    }

    private Funding funding(
            Long fundingId,
            TripType tripType,
            FundingStatus status
    ) {
        Funding funding = Funding.create(
                member(),
                "Football Match Bus",
                "Ride together",
                DEPARTURE_TIME.toLocalDate(),
                BusType.BUS_45,
                20,
                500000,
                tripType
        );
        ReflectionTestUtils.setField(funding, "fundingId", fundingId);
        ReflectionTestUtils.setField(funding, "status", status);
        ReflectionTestUtils.setField(funding, "createdAt", DEPARTURE_TIME.minusDays(1));
        return funding;
    }

    private Member member() {
        return Member.builder()
                .memberId(1L)
                .email("test@test.com")
                .password("1234")
                .name("test")
                .nickname("test")
                .phoneNumber("01012341234")
                .status(MemberStatus.ACTIVE)
                .createdAt(DEPARTURE_TIME.minusDays(10))
                .build();
    }
}
