package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.chatroom.entity.ChatRoom;
import com.back.team9.moyeota.domain.chatroom.repository.ChatRoomRepository;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FundingServiceTest {

    private static final LocalDateTime DEFAULT_DEPARTURE_TIME =
            LocalDateTime.of(2027, 6, 20, 8, 0);
    private static final LocalDateTime DEFAULT_RETURN_TIME =
            LocalDateTime.of(2027, 6, 20, 23, 0);

    @Autowired
    private FundingService fundingService;

    @Autowired
    private FundingRepository fundingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PathinfoRepository pathinfoRepository;

    @Autowired
    private PathinfoService pathinfoService;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Test
    @DisplayName("편도 펀딩 생성 성공")
    void createOneWayFunding_success() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = oneWayCreateRequest();

        // When
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), request);

        // Then
        Funding funding = findFunding(response.fundingId());
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());
        Pathinfo outbound = findPathinfo(pathinfos, Direction.OUTBOUND);

        assertThat(funding.getTitle()).isEqualTo(request.title());
        assertThat(pathinfos).hasSize(1);
        assertHostSeat(outbound, "1A", member);
    }

    @Test
    @DisplayName("펀딩 생성 시 채팅방 자동 생성 및 방장 일치")
    void createFunding_createsChatRoomWithSameHost() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = oneWayCreateRequest();

        // When
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), request);

        // Then
        ChatRoom chatRoom = chatRoomRepository
                .findByFundingFundingId(response.fundingId())
                .orElseThrow();

        assertThat(chatRoom.getFunding().getFundingId())
                .isEqualTo(response.fundingId());
        assertThat(chatRoom.getFunding().getMember().getMemberId())
                .isEqualTo(member.getMemberId());
    }

    @Test
    @DisplayName("왕복 펀딩 생성 성공")
    void createRoundFunding_success() {
        // Given
        Member member = saveMember();

        // When
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // Then
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());

        assertThat(pathinfos).hasSize(2);
        assertHostSeat(findPathinfo(pathinfos, Direction.OUTBOUND), "1A", member);
        assertHostSeat(findPathinfo(pathinfos, Direction.RETURN), "1B", member);
    }

    @Test
    @DisplayName("왕복 펀딩 생성 - 방장 복귀 좌석이 없으면 예외")
    void createFunding_whenRoundTripHasNoHostReturnSeat_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                "1A",
                null,
                roundRoute()
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUND_TRIP_SEAT_REQUIRED);
    }

    @Test
    @DisplayName("펀딩 생성 - 방장 좌석번호가 존재하지 않으면 예외")
    void createFunding_whenHostSeatDoesNotExist_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                "99Z",
                null,
                oneWayRoute()
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEAT_NOT_FOUND);
    }

    @Test
    @DisplayName("왕복 펀딩 - 복귀 시간 없음 예외")
    void createFunding_whenRoundTripHasNoReturnTime_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.SEOUL)
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    @Test
    @DisplayName("펀딩 상세 조회 성공")
    void getFunding_success() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        // When
        FundingDetailResponse result = fundingService.getFunding(response.fundingId(), member.getMemberId());

        // Then
        assertThat(result.fundingId()).isEqualTo(response.fundingId());
        assertThat(result.chatRoomId()).isNotNull();
        assertThat(result.title()).isEqualTo("Football Match Bus");
        assertThat(result.pathinfos()).hasSize(1);
    }

    @Test
    @DisplayName("펀딩 목록 조회 성공")
    void getFundingList_success() {
        // Given
        Member member = saveMember();
        fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // When
        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(
                        emptyCondition(),
                        PageRequest.of(0, 10)
                );

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("펀딩 목록 조회 - 현재 참가자 수 반영")
    void getFundingList_returnsCurrentParticipants() {
        // Given
        Member host = saveMember();
        Member participant = saveMember("participant@test.com", "participant");
        FundingCreateResponse response =
                fundingService.createFunding(host.getMemberId(), oneWayCreateRequest());
        Funding funding = findFunding(response.fundingId());
        Pathinfo outbound = findPathinfo(response.fundingId(), Direction.OUTBOUND);
        Seat seat = seatRepository.findByPathinfo_PathinfoId(outbound.getPathinfoId())
                .stream()
                .filter(candidate -> candidate.getSeatNumber().equals("1B"))
                .findFirst()
                .orElseThrow();
        participationRepository.save(
                Participation.create(funding, participant, seat, null)
        );

        // When
        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(emptyCondition(), PageRequest.of(0, 10));

        // Then
        assertThat(result.content())
                .filteredOn(content -> content.fundingId().equals(response.fundingId()))
                .singleElement()
                .extracting(FundingListResponse::currentParticipants)
                .isEqualTo(1);
    }

    @Test
    @DisplayName("펀딩 전체 수정 성공")
    void updateFunding_success() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        // Then
        Funding funding = findFunding(response.fundingId());
        assertThat(funding.getTitle()).isEqualTo("Updated Title");
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_25);
    }

    @Test
    @DisplayName("펀딩 취소 성공")
    void cancelFunding_success() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        // When
        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        // Then
        Funding funding = findFunding(response.fundingId());
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CANCELLED);
    }

    @Test
    @DisplayName("편도->왕복 수정 오는 노선 생성")
    void updateFunding_fromOneWayToRound_createsReturnPathinfo() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                roundUpdateRequest()
        );

        // Then
        assertThat(findPathinfos(response.fundingId())).hasSize(2);
    }

    @Test
    @DisplayName("왕복->편도 수정 오는 노선 취소")
    void updateFunding_fromRoundToOneWay_cancelsReturnPathinfo() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        // Then
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());
        Pathinfo outbound = findPathinfo(pathinfos, Direction.OUTBOUND);
        Pathinfo returned = findPathinfo(pathinfos, Direction.RETURN);

        assertThat(pathinfos).hasSize(2);
        assertThat(outbound.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);
        assertThat(pathinfoService.getPathinfoResponses(response.fundingId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("펀딩 취소 시 모든 노선 취소")
    void cancelFunding_cancelsAllPathinfos() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // When
        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        // Then
        assertThat(findPathinfos(response.fundingId()))
                .allMatch(pathinfo -> pathinfo.getStatus() == PathinfoStatus.CANCELLED);
    }

    @Test
    @DisplayName("왕복 노선 수정 성공")
    void updateFunding_roundPathinfo_success() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        DEFAULT_RETURN_TIME,
                        "Gangnam Station",
                        Region.DAEJEON,
                        "Seoul Stadium",
                        Region.SEOUL
                )
        );

        // When
        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

        // Then
        assertThat(findPathinfos(response.fundingId()))
                .extracting(Pathinfo::getDepartureAddress)
                .contains("Gangnam Station");
    }

    @Test
    @DisplayName("수정 - 참가자가 없으면 지역 변경 시 총금액 재계산")
    void updateFunding_whenRegionChanges_recalculatesTotalPrice() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        null,
                        Region.DAEJEON,
                        Region.SEOUL
                )
        );

        // When
        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

        // Then
        Funding funding = findFunding(response.fundingId());
        Pathinfo outbound = findPathinfo(response.fundingId(), Direction.OUTBOUND);

        assertThat(funding.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(774400));
        assertThat(outbound.getDepartureRegion()).isEqualTo(Region.DAEJEON);
        assertThat(outbound.getArrivalRegion()).isEqualTo(Region.SEOUL);
    }

    @Test
    @DisplayName("수정 - 편도에서 왕복으로 변경 시 총금액을 왕복 금액으로 재계산")
    void updateFunding_fromOneWayToRound_recalculatesRoundTripTotalPrice() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                roundRoute()
        );

        // When
        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

        // Then
        Funding funding = findFunding(response.fundingId());
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());

        assertThat(funding.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(1452000));
        assertThat(funding.getTripType()).isEqualTo(TripType.ROUND);
        assertThat(pathinfos).hasSize(2);
    }

    @Test
    @DisplayName("수정 - 왕복에서 편도로 변경 시 총금액을 편도 금액으로 재계산")
    void updateFunding_fromRoundToOneWay_recalculatesOneWayTotalPrice() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                oneWayRoute()
        );

        // When
        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

        // Then
        Funding funding = findFunding(response.fundingId());
        Pathinfo returned = findPathinfo(response.fundingId(), Direction.RETURN);

        assertThat(funding.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(726000));
        assertThat(funding.getTripType()).isEqualTo(TripType.ONE_WAY);
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);
    }

    @Test
    @DisplayName("생성 - 출발지역-도착지역 동일 예외")
    void createFunding_whenDepartureAndArrivalRegionsAreSame_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.INCHEON)
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_DEPARTURE_ARRIVAL);
    }

    @Test
    @DisplayName("수정 - 출발지역-도착지역 동일 예외")
    void updateFunding_whenDepartureAndArrivalRegionsAreSame_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.INCHEON)
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(member.getMemberId(), response.fundingId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_DEPARTURE_ARRIVAL);
    }

    @Test
    @DisplayName("생성 - 출발일 14일 미만 예외")
    void createFunding_whenDepartureDateIsTooSoon_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                route(LocalDateTime.now().plusDays(7), null, Region.INCHEON, Region.SEOUL)
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTURE_DATE_TOO_SOON);
    }

    @Test
    @DisplayName("생성 - 출발시간이 복귀시간보다 늦음 예외")
    void createFunding_whenReturnTimeIsBeforeDepartureTime_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 20, 5, 0),
                        Region.INCHEON,
                        Region.SEOUL
                )
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_TIME_BEFORE_OUTBOUND);
    }

    @Test
    @DisplayName("생성 - 왕복 날짜 다름 예외")
    void createFunding_whenReturnDateDiffersFromDepartureDate_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 22, 23, 0),
                        Region.INCHEON,
                        Region.SEOUL
                )
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_DATE_MUST_MATCH_OUTBOUND);
    }

    @Test
    @DisplayName("수정 - 출발일 14일 미만 예외")
    void updateFunding_whenDepartureDateIsTooSoon_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                route(LocalDateTime.now().plusDays(7), null, Region.INCHEON, Region.SEOUL)
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(member.getMemberId(), response.fundingId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTURE_DATE_TOO_SOON);
    }

    @Test
    @DisplayName("수정 - 출발시간이 복귀시간보다 늦음 예외")
    void updateFunding_whenReturnTimeIsBeforeDepartureTime_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 20, 5, 0),
                        Region.INCHEON,
                        Region.SEOUL
                )
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(member.getMemberId(), response.fundingId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_TIME_BEFORE_OUTBOUND);
    }

    @Test
    @DisplayName("수정 - 왕복 날짜 다름 예외")
    void updateFunding_whenReturnDateDiffersFromDepartureDate_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 22, 23, 0),
                        Region.INCHEON,
                        Region.SEOUL
                )
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(member.getMemberId(), response.fundingId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_DATE_MUST_MATCH_OUTBOUND);
    }

    @Test
    @DisplayName("생성 - 펀딩/노선 버스 타입 일치")
    void createFunding_syncsBusTypeToAllPathinfos() {
        // Given
        Member member = saveMember();

        // When
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // Then
        Funding funding = findFunding(response.fundingId());
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());

        assertThat(pathinfos).hasSize(2);
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_45);
        assertThat(pathinfos)
                .extracting(Pathinfo::getBusType)
                .containsOnly(funding.getBusType());
    }

    @Test
    @DisplayName("수정 - 펀딩/노선 버스 타입 일치")
    void updateFunding_syncsBusTypeToAllPathinfos() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        // Then
        Funding funding = findFunding(response.fundingId());
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());

        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_25);
        assertThat(pathinfos)
                .extracting(Pathinfo::getBusType)
                .containsOnly(BusType.BUS_25);
    }

    @Test
    @DisplayName("수정 - 버스 타입만 변경해도 노선 버스 타입 동기화")
    void updateFunding_whenOnlyBusTypeChanges_syncsAllPathinfoBusTypes() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        Funding fundingBefore = findFunding(response.fundingId());
        assertThat(fundingBefore.getBusType()).isEqualTo(BusType.BUS_45);
        assertThat(findPathinfos(response.fundingId()))
                .extracting(Pathinfo::getBusType)
                .containsOnly(BusType.BUS_45);

        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                20,
                TripType.ROUND,
                roundRoute()
        );

        // When
        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

        // Then
        Funding fundingAfter = findFunding(response.fundingId());
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());

        assertThat(fundingAfter.getBusType()).isEqualTo(BusType.BUS_25);
        assertThat(pathinfos).hasSize(2);
        assertThat(pathinfos)
                .extracting(Pathinfo::getBusType)
                .containsOnly(BusType.BUS_25);
        assertThat(pathinfos.get(0).getBusType()).isEqualTo(fundingAfter.getBusType());
        assertThat(pathinfos.get(1).getBusType()).isEqualTo(fundingAfter.getBusType());
    }

    @Test
    @DisplayName("기본 목록에서 취소 펀딩 제외")
    void getFundingList_defaultStatusExcludesCancelledFunding() {
        // Given
        Member member = saveMember();
        FundingCreateResponse active =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingCreateResponse cancelled =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.cancelFunding(member.getMemberId(), cancelled.fundingId());

        // When
        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(emptyCondition(), PageRequest.of(0, 10));

        // Then
        assertThat(result.content())
                .extracting(FundingListResponse::fundingId)
                .containsExactly(active.fundingId());
    }

    @Test
    @DisplayName("취소 상태 목록에서 추가 시 취소된 노선 포함")
    void getFundingList_statusFilterIncludesCancelledPathinfo() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        // When
        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(
                        new FundingSearchCondition(
                                List.of(FundingStatus.CANCELLED),
                                null,
                                null,
                                null
                        ),
                        PageRequest.of(0, 10)
                );

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).fundingId()).isEqualTo(response.fundingId());
        assertThat(result.content().get(0).departureAddress()).isNotNull();
        assertThat(result.content().get(0).arrivalAddress()).isNotNull();
        assertThat(result.content().get(0).departureTime()).isNotNull();
    }

    @Test
    @DisplayName("펀딩 목록 - 상태 필터 동작")
    void getFundingList_filtersByRegionDateAndStatus() {
        // Given
        Member member = saveMember();
        FundingCreateResponse expected =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRequest(
                                BusType.BUS_45,
                                20,
                                TripType.ONE_WAY,
                                route(
                                        LocalDateTime.of(2027, 6, 21, 8, 0),
                                        null,
                                        Region.SEOUL,
                                        Region.INCHEON
                                )
                        )
                );

        fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        // When
        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(
                        new FundingSearchCondition(
                                List.of(FundingStatus.RECRUITING),
                                LocalDate.of(2027, 6, 21),
                                Region.SEOUL,
                                Region.INCHEON
                        ),
                        PageRequest.of(0, 10)
                );

        // Then
        assertThat(result.content())
                .extracting(FundingListResponse::fundingId)
                .containsExactly(expected.fundingId());
    }

    @Test
    @DisplayName("수정 - 왕복->편도->왕복 오는 노선 재활성화")
    void updateFunding_roundToOneWayToRound_reactivatesReturnPathinfo() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        Pathinfo cancelledReturn = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(cancelledReturn.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                roundUpdateRequest()
        );

        // Then
        Pathinfo returned = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(pathinfoService.getPathinfoResponses(response.fundingId())).hasSize(2);
    }

    @Test
    @DisplayName("펀딩 생성/수정 - 최종 버스 타입과 왕복 여부에 맞게 좌석 정합성을 유지한다")
    void updateFunding_keepsSeatConsistencyAfterRepeatedChanges() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        Pathinfo outbound = findPathinfo(response.fundingId(), Direction.OUTBOUND);
        assertSeatsMatchBusType(outbound);

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                roundUpdateRequest()
        );

        // Then
        Pathinfo roundOutbound = findPathinfo(response.fundingId(), Direction.OUTBOUND);
        Pathinfo roundReturn = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(roundOutbound.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(roundReturn.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertSeatsMatchBusType(roundOutbound);
        assertSeatsMatchBusType(roundReturn);
        assertHostSeat(roundOutbound, "1A", member);
        assertHostSeat(roundReturn, "1B", member);

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                updateRequest(
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        oneWayRoute()
                )
        );

        // Then
        Pathinfo oneWayOutbound = findPathinfo(response.fundingId(), Direction.OUTBOUND);
        Pathinfo cancelledReturn = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(oneWayOutbound.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(cancelledReturn.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);
        assertSeatsMatchBusType(oneWayOutbound);
        assertNoSeats(cancelledReturn);
        assertHostSeat(oneWayOutbound, "1A", member);

        // When
        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                roundUpdateRequest()
        );

        // Then
        Pathinfo restoredOutbound = findPathinfo(response.fundingId(), Direction.OUTBOUND);
        Pathinfo restoredReturn = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(restoredOutbound.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(restoredReturn.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertSeatsMatchBusType(restoredOutbound);
        assertSeatsMatchBusType(restoredReturn);
        assertHostSeat(restoredOutbound, "1A", member);
        assertHostSeat(restoredReturn, "1B", member);
    }

    @Test
    @DisplayName("펀딩 수정 - 왕복으로 변경 시 방장 복귀 좌석이 없으면 예외")
    void updateFunding_whenRoundTripHasNoHostReturnSeat_throwsException() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ROUND,
                "1A",
                null,
                roundRoute()
        );

        // When / Then
        assertThatThrownBy(() ->
                fundingService.updateFunding(member.getMemberId(), response.fundingId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUND_TRIP_SEAT_REQUIRED);
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 취소된 펀딩 상세 조회 시 최종 편도 기준 노선 조회")
    void getFunding_cancelledOneWayDetail_usesFinalTripType() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );
        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        // When
        FundingDetailResponse result = fundingService.getFunding(response.fundingId(), member.getMemberId());

        // Then
        assertThat(result.tripType()).isEqualTo(TripType.ONE_WAY);
        assertThat(result.pathinfos()).hasSize(1);
        assertThat(result.pathinfos().get(0).direction()).isEqualTo(Direction.OUTBOUND);
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 실패한 펀딩 상세 조회 시 최종 편도 기준 노선 조회")
    void getFunding_failedRoundDetail_usesFinalTripType() {
        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        Funding funding = findFunding(response.fundingId());
        ReflectionTestUtils.setField(funding, "status", FundingStatus.FAILED);
        pathinfoService.cancelPathinfos(response.fundingId());

        // When
        FundingDetailResponse result = fundingService.getFunding(response.fundingId(), member.getMemberId());

        // Then
        assertThat(result.status()).isEqualTo(FundingStatus.FAILED);
        assertThat(result.tripType()).isEqualTo(TripType.ROUND);
        assertThat(result.pathinfos()).hasSize(2);
    }


    private FundingCreateRequest oneWayCreateRequest() {
        return createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                oneWayRoute()
        );
    }

    private FundingCreateRequest roundCreateRequest() {
        return createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                roundRoute()
        );
    }

    private FundingCreateRequest createRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            RouteRequest route
    ) {
        return createRequest(
                busType,
                minParticipants,
                tripType,
                "1A",
                tripType == TripType.ROUND ? "1B" : null,
                route
        );
    }

    private FundingCreateRequest createRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            String hostOutboundSeatNumber,
            String hostReturnSeatNumber,
            RouteRequest route
    ) {
        return new FundingCreateRequest(
                "Football Match Bus",
                "Ride together",
                busType,
                minParticipants,
                tripType,
                hostOutboundSeatNumber,
                hostReturnSeatNumber,
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

    private FundingUpdateRequest roundUpdateRequest() {
        return updateRequest(
                BusType.BUS_25,
                10,
                TripType.ROUND,
                roundRoute()
        );
    }

    private FundingUpdateRequest updateRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            RouteRequest route
    ) {
        return updateRequest(
                busType,
                minParticipants,
                tripType,
                "1A",
                tripType == TripType.ROUND ? "1B" : null,
                route
        );
    }

    private FundingUpdateRequest updateRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            String hostOutboundSeatNumber,
            String hostReturnSeatNumber,
            RouteRequest route
    ) {
        return new FundingUpdateRequest(
                "Updated Title",
                "Updated Content",
                busType,
                minParticipants,
                tripType,
                hostOutboundSeatNumber,
                hostReturnSeatNumber,
                route
        );
    }

    private RouteRequest oneWayRoute() {
        return route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.SEOUL);
    }

    private RouteRequest roundRoute() {
        return route(DEFAULT_DEPARTURE_TIME, DEFAULT_RETURN_TIME, Region.INCHEON, Region.SEOUL);
    }

    private RouteRequest route(
            LocalDateTime departureTime,
            LocalDateTime returnTime,
            Region departureRegion,
            Region arrivalRegion
    ) {
        return route(
                departureTime,
                returnTime,
                "Incheon Terminal",
                departureRegion,
                "Seoul Stadium",
                arrivalRegion
        );
    }

    private RouteRequest route(
            LocalDateTime departureTime,
            LocalDateTime returnTime,
            String departureAddress,
            Region departureRegion,
            String arrivalAddress,
            Region arrivalRegion
    ) {
        return new RouteRequest(
                departureTime,
                returnTime,
                departureAddress,
                departureRegion,
                arrivalAddress,
                arrivalRegion
        );
    }

    private FundingSearchCondition emptyCondition() {
        return new FundingSearchCondition(null, null, null, null);
    }

    private Funding findFunding(Long fundingId) {
        return fundingRepository.findById(fundingId).orElseThrow();
    }

    private List<Pathinfo> findPathinfos(Long fundingId) {
        return pathinfoRepository.findByFunding_FundingId(fundingId);
    }

    private Pathinfo findPathinfo(Long fundingId, Direction direction) {
        return pathinfoRepository
                .findByFunding_FundingIdAndDirection(fundingId, direction)
                .orElseThrow();
    }

    private Pathinfo findPathinfo(List<Pathinfo> pathinfos, Direction direction) {
        return pathinfos.stream()
                .filter(pathinfo -> pathinfo.getDirection() == direction)
                .findFirst()
                .orElseThrow();
    }

    private void assertSeatsMatchBusType(Pathinfo pathinfo) {
        List<Seat> seats =
                seatRepository.findByPathinfo_PathinfoId(pathinfo.getPathinfoId());

        assertThat(seats).hasSize(pathinfo.getBusType().getCapacity() + 1);
        assertThat(seats)
                .extracting(Seat::getSeatNumber)
                .containsExactlyInAnyOrderElementsOf(
                        expectedSeatNumbers(pathinfo.getBusType())
                );
    }

    private void assertNoSeats(Pathinfo pathinfo) {
        assertThat(seatRepository.findByPathinfo_PathinfoId(pathinfo.getPathinfoId()))
                .isEmpty();
    }

    private void assertHostSeat(
            Pathinfo pathinfo,
            String seatNumber,
            Member host
    ) {
        Seat seat = seatRepository.findByPathinfo_PathinfoId(pathinfo.getPathinfoId())
                .stream()
                .filter(candidate -> candidate.getSeatNumber().equals(seatNumber))
                .findFirst()
                .orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seat.getHostMember().getMemberId())
                .isEqualTo(host.getMemberId());
    }

    private List<String> expectedSeatNumbers(BusType busType) {
        int maxRow;
        String[] columns;

        if (busType == BusType.BUS_25) {
            maxRow = 8;
            columns = new String[]{"A", "B", "C"};
        } else {
            maxRow = 11;
            columns = new String[]{"A", "B", "C", "D"};
        }

        List<String> seatNumbers = new ArrayList<>();
        for (int row = 1; row <= maxRow; row++) {
            for (String column : columns) {
                seatNumbers.add(row + column);
            }
        }

        return seatNumbers;
    }

    private Member saveMember() {
        return saveMember("test@test.com", "test");
    }

    private Member saveMember(String email, String nickname) {
        Member member = Member.builder()
                .email(email)
                .password("1234")
                .name("test")
                .nickname(nickname)
                .phoneNumber("01012341234")
                .status(MemberStatus.ACTIVE)
                .build();
        return memberRepository.save(member);
    }

}
