package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoomStatus;
import com.back.team9.moyeota.domain.chatroom.repository.ChatRoomRepository;
import com.back.team9.moyeota.domain.funding.dto.FundingCreateRequest;
import com.back.team9.moyeota.domain.funding.dto.FundingCreateResponse;
import com.back.team9.moyeota.domain.funding.dto.FundingSearchCondition;
import com.back.team9.moyeota.domain.funding.dto.FundingUpdateRequest;
import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.event.FundingCreatedEvent;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.funding.validator.FundingValidator;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.dto.PathinfoResponse;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.domain.seat.service.SeatService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
class FundingServiceUnitTest {

    private static final LocalDateTime DEPARTURE_TIME =
            LocalDateTime.of(2027, 6, 20, 8, 0);

    @InjectMocks
    private FundingService fundingService;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PathinfoService pathinfoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private FundingValidator fundingValidator;

    @Mock
    private SeatService seatService;

    @Test
    @DisplayName("펀딩 생성 성공")
    void createFunding_success() {
        // Given
        Member member = member(1L);
        FundingCreateRequest request = createRequest();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(fundingRepository.save(any(Funding.class))).willAnswer(invocation -> {
            Funding funding = invocation.getArgument(0);
            ReflectionTestUtils.setField(funding, "fundingId", 10L);
            ReflectionTestUtils.setField(funding, "createdAt", DEPARTURE_TIME.minusDays(1));
            return funding;
        });

        // When
        FundingCreateResponse response =
                fundingService.createFunding(1L, request);

        // Then
        assertThat(response.fundingId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(FundingStatus.RECRUITING);

        ArgumentCaptor<Funding> fundingCaptor =
                ArgumentCaptor.forClass(Funding.class);
        verify(fundingRepository).save(fundingCaptor.capture());
        Funding savedFunding = fundingCaptor.getValue();

        assertThat(savedFunding.getMember()).isEqualTo(member);
        assertThat(savedFunding.getTitle()).isEqualTo(request.title());
        assertThat(savedFunding.getDepartureDate())
                .isEqualTo(request.route().departureTime().toLocalDate());
        assertThat(savedFunding.getMaxParticipants())
                .isEqualTo(BusType.BUS_45.getCapacity());

        verify(fundingValidator)
                .validateFundingRequest(20, BusType.BUS_45);
        verify(pathinfoService)
                .createPathinfos(savedFunding, TripType.ONE_WAY, request.route());
        ArgumentCaptor<FundingCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(FundingCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().funding()).isEqualTo(savedFunding);
    }

    @Test
    @DisplayName("펀딩 생성 - 회원이 없으면 예외")
    void createFunding_whenMemberDoesNotExist_throwsException() {
        // Given
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(999L, createRequest())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verifyNoInteractions(fundingRepository, pathinfoService, eventPublisher, fundingValidator);
    }

    @Test
    @DisplayName("펀딩 생성 - 검증 실패 시 저장하지 않는다")
    void createFunding_whenValidationFails_throwsException() {
        // Given
        Member member = member(1L);
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                70,
                TripType.ONE_WAY
        );

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        willThrow(new BusinessException(ErrorCode.FUNDING_MIN_INVALID))
                .given(fundingValidator)
                .validateFundingRequest(70, BusType.BUS_45);

        // When / Then
        assertThatThrownBy(() -> fundingService.createFunding(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_MIN_INVALID);

        verify(fundingRepository, never()).save(any());
        verifyNoInteractions(pathinfoService, eventPublisher);
    }

    @Test
    @DisplayName("펀딩 상세 조회 성공")
    void getFunding_success() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);
        PathinfoResponse pathinfoResponse = pathinfoResponse();

        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        given(participationRepository.countByFunding_FundingIdAndStatus(
                10L,
                ParticipationStatus.ACTIVE
        )).willReturn(3L);
        given(pathinfoService.getPathinfoResponsesForDetail(funding))
                .willReturn(List.of(pathinfoResponse));
        given(chatRoomRepository.findByFundingFundingId(10L))
                .willReturn(Optional.of(chatRoom(100L, funding)));

        // When
        var response = fundingService.getFunding(10L);

        // Then
        assertThat(response.fundingId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Football Match Bus");
        assertThat(response.chatRoomId()).isEqualTo(100L);
        assertThat(response.pathinfos()).containsExactly(pathinfoResponse);
        assertThat(response.currentParticipants()).isEqualTo(3);
        assertThat(response.isHost()).isFalse();
        assertThat(response.isJoined()).isFalse();
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 펀딩이 없으면 예외")
    void getFunding_whenFundingDoesNotExist_throwsException() {
        // Given
        given(fundingRepository.findById(999L)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> fundingService.getFunding(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);

        verifyNoInteractions(pathinfoService);
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 채팅방이 없으면 예외")
    void getFunding_whenChatRoomDoesNotExist_throwsException() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);

        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        given(participationRepository.countByFunding_FundingIdAndStatus(
                10L,
                ParticipationStatus.ACTIVE
        )).willReturn(3L);
        given(pathinfoService.getPathinfoResponsesForDetail(funding))
                .willReturn(List.of(pathinfoResponse()));
        given(chatRoomRepository.findByFundingFundingId(10L))
                .willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> fundingService.getFunding(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("펀딩 수정 성공")
    void updateFunding_success() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);
        FundingUpdateRequest request = updateRequest();

        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        given(participationRepository.countByFunding_FundingIdAndStatus(
                10L,
                ParticipationStatus.ACTIVE
        )).willReturn(0L);

        // When
        fundingService.updateFunding(1L, 10L, request);

        // Then
        assertThat(funding.getTitle()).isEqualTo("Updated Title");
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_25);
        assertThat(funding.getMinParticipants()).isEqualTo(10);
        assertThat(funding.getMaxParticipants())
                .isEqualTo(BusType.BUS_25.getCapacity());
        assertThat(funding.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(495000));

        verify(fundingValidator).validateHost(funding, 1L);
        verify(fundingValidator).validateUpdatable(funding);
        verify(fundingValidator)
                .validateFundingRequest(10, BusType.BUS_25);
        verify(pathinfoService)
                .updatePathinfos(funding, TripType.ONE_WAY, request.route());
        verify(pathinfoService).syncBusType(10L, BusType.BUS_25);
        verify(seatService).recreateSeatsForActivePathinfos(10L);
    }

    @Test
    @DisplayName("펀딩 수정 - 참가자가 없으면 지역 변경 시 총금액을 재계산한다")
    void updateFunding_whenNoParticipantsAndRegionChanges_recalculatesTotalPrice() {
        // Given
        Funding funding = funding(
                10L,
                member(1L),
                FundingStatus.RECRUITING,
                BusType.BUS_25,
                BigDecimal.valueOf(495000)
        );
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                route(Region.DAEJEON, Region.SEOUL)
        );

        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        given(participationRepository.countByFunding_FundingIdAndStatus(
                10L,
                ParticipationStatus.ACTIVE
        )).willReturn(0L);

        // When
        fundingService.updateFunding(1L, 10L, request);

        // Then
        assertThat(funding.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(550000));
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_25);

        verify(fundingValidator).validateHost(funding, 1L);
        verify(fundingValidator).validateUpdatable(funding);
        verify(fundingValidator).validateFundingRequest(10, BusType.BUS_25);
        verify(pathinfoService)
                .updatePathinfos(funding, TripType.ONE_WAY, request.route());
        verify(pathinfoService).syncBusType(10L, BusType.BUS_25);
        verify(seatService, never()).recreateSeatsForActivePathinfos(10L);
    }

    @Test
    @DisplayName("펀딩 수정 - 펀딩이 없으면 예외")
    void updateFunding_whenFundingDoesNotExist_throwsException() {
        // Given
        given(fundingRepository.findById(999L)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(1L, 999L, updateRequest())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);

        verifyNoInteractions(pathinfoService, fundingValidator);
    }

    @Test
    @DisplayName("펀딩 수정 - 방장이 아니면 예외")
    void updateFunding_whenMemberIsNotHost_throwsException() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);
        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        willThrow(new BusinessException(ErrorCode.FUNDING_FORBIDDEN))
                .given(fundingValidator)
                .validateHost(funding, 2L);

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(2L, 10L, updateRequest())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_FORBIDDEN);

        verify(fundingValidator, never()).validateUpdatable(funding);
        verifyNoInteractions(pathinfoService);
    }

    @Test
    @DisplayName("펀딩 수정 - 수정 불가 상태면 예외")
    void updateFunding_whenFundingIsNotUpdatable_throwsException() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.CONFIRMED);
        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        willThrow(new BusinessException(ErrorCode.FUNDING_RESTRICTED_UPDATE))
                .given(fundingValidator)
                .validateUpdatable(funding);

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(1L, 10L, updateRequest())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_RESTRICTED_UPDATE);

        verify(fundingValidator, never())
                .validateFundingRequest(10, BusType.BUS_25);
        verifyNoInteractions(pathinfoService);
    }

    @Test
    @DisplayName("펀딩 취소 성공")
    void cancelFunding_success() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);
        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));

        // When
        fundingService.cancelFunding(1L, 10L);

        // Then
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CANCELLED);
        verify(fundingValidator).validateHost(funding, 1L);
        verify(fundingValidator).validateUpdatable(funding);
        verify(pathinfoService).cancelPathinfos(10L);
    }

    @Test
    @DisplayName("펀딩 취소 - 이미 취소된 펀딩이면 예외")
    void cancelFunding_whenAlreadyCancelled_throwsException() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.CANCELLED);
        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));

        // When / Then
        assertThatThrownBy(() -> fundingService.cancelFunding(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_ALREADY_CANCELLED);

        verify(fundingValidator).validateHost(funding, 1L);
        verify(fundingValidator, never()).validateUpdatable(funding);
        verify(pathinfoService, never()).cancelPathinfos(10L);
    }

    @Test
    @DisplayName("펀딩 취소 - 방장이 아니면 예외")
    void cancelFunding_whenMemberIsNotHost_throwsException() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);
        given(fundingRepository.findById(10L)).willReturn(Optional.of(funding));
        willThrow(new BusinessException(ErrorCode.FUNDING_FORBIDDEN))
                .given(fundingValidator)
                .validateHost(funding, 2L);

        // When / Then
        assertThatThrownBy(() -> fundingService.cancelFunding(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_FORBIDDEN);

        verify(fundingValidator, never()).validateUpdatable(funding);
        verify(pathinfoService, never()).cancelPathinfos(10L);
    }

    @Test
    @DisplayName("펀딩 목록 조회 - 조회된 펀딩의 출발 경로를 붙여 응답")
    void getFundingList_success() {
        // Given
        Funding funding = funding(10L, member(1L), FundingStatus.RECRUITING);
        Pathinfo pathinfo = Pathinfo.create(
                funding,
                DEPARTURE_TIME,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL,
                Direction.OUTBOUND
        );
        FundingSearchCondition condition =
                new FundingSearchCondition(null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 10);

        given(fundingRepository.findPageByCondition(condition, pageable))
                .willReturn(new PageImpl<>(List.of(funding), pageable, 1));
        given(pathinfoService.findByFundingIdsAndDirection(
                List.of(10L),
                Direction.OUTBOUND
        )).willReturn(List.of(pathinfo));
        given(participationRepository.countByFundingIdsAndStatus(
                List.of(10L),
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participantCount(10L, 7L)));

        // When
        PageResponse<com.back.team9.moyeota.domain.funding.dto.FundingListResponse> response =
                fundingService.getFundingList(condition, pageable);

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).currentParticipants()).isEqualTo(7);
        verify(pathinfoService)
                .findByFundingIdsAndDirection(List.of(10L), Direction.OUTBOUND);
        verify(participationRepository)
                .countByFundingIdsAndStatus(List.of(10L), ParticipationStatus.ACTIVE);
    }

    private FundingCreateRequest createRequest() {
        return createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY
        );
    }

    private FundingCreateRequest createRequest(
            BusType busType,
            int minParticipants,
            TripType tripType
    ) {
        return new FundingCreateRequest(
                "Football Match Bus",
                "Ride together",
                busType,
                minParticipants,
                tripType,
                route()
        );
    }

    private FundingUpdateRequest updateRequest() {
        return updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                route()
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
                route
        );
    }

    private RouteRequest route() {
        return route(Region.INCHEON, Region.SEOUL);
    }

    private RouteRequest route(
            Region departureRegion,
            Region arrivalRegion
    ) {
        return new RouteRequest(
                DEPARTURE_TIME,
                null,
                "Incheon Terminal",
                departureRegion,
                "Seoul Stadium",
                arrivalRegion
        );
    }

    private PathinfoResponse pathinfoResponse() {
        return new PathinfoResponse(
                100L,
                DEPARTURE_TIME,
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL,
                PathinfoStatus.PENDING,
                Direction.OUTBOUND
        );
    }

    private ChatRoom chatRoom(Long chatRoomId, Funding funding) {
        return ChatRoom.builder()
                .chatroomId(chatRoomId)
                .funding(funding)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(DEPARTURE_TIME.minusDays(1))
                .build();
    }

    private Funding funding(
            Long fundingId,
            Member member,
            FundingStatus status
    ) {
        return funding(
                fundingId,
                member,
                status,
                BusType.BUS_45,
                BigDecimal.valueOf(500000)
        );
    }

    private Funding funding(
            Long fundingId,
            Member member,
            FundingStatus status,
            BusType busType,
            BigDecimal totalPrice
    ) {
        Funding funding = Funding.create(
                member,
                "Football Match Bus",
                "Ride together",
                DEPARTURE_TIME.toLocalDate(),
                busType,
                20,
                totalPrice,
                TripType.ONE_WAY
        );
        ReflectionTestUtils.setField(funding, "fundingId", fundingId);
        ReflectionTestUtils.setField(funding, "status", status);
        ReflectionTestUtils.setField(funding, "createdAt", DEPARTURE_TIME.minusDays(1));
        return funding;
    }

    private Member member(Long memberId) {
        return Member.builder()
                .memberId(memberId)
                .email("test@test.com")
                .password("1234")
                .name("test")
                .nickname("test")
                .phoneNumber("01012341234")
                .status(MemberStatus.ACTIVE)
                .createdAt(DEPARTURE_TIME.minusDays(10))
                .build();
    }

    private ParticipationRepository.FundingParticipationCount participantCount(
            Long fundingId,
            Long count
    ) {
        return new ParticipationRepository.FundingParticipationCount() {
            @Override
            public Long getFundingId() {
                return fundingId;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
