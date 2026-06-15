package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.dto.FundingCreateRequest;
import com.back.team9.moyeota.domain.funding.dto.FundingCreateResponse;
import com.back.team9.moyeota.domain.funding.dto.FundingDetailResponse;
import com.back.team9.moyeota.domain.funding.dto.FundingListResponse;
import com.back.team9.moyeota.domain.funding.dto.FundingSearchCondition;
import com.back.team9.moyeota.domain.funding.dto.FundingUpdateRequest;
import com.back.team9.moyeota.domain.funding.dto.RouteRequest;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import com.back.team9.moyeota.domain.pathinfo.service.PathinfoService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
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

    @Test
    @DisplayName("편도 펀딩 생성 성공")
    void createOneWayFunding_success() {
        Member member = saveMember();
        FundingCreateRequest request = oneWayCreateRequest();

        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), request);

        Funding funding = findFunding(response.fundingId());
        List<Pathinfo> pathinfos = findPathinfos(response.fundingId());

        assertThat(funding.getTitle()).isEqualTo(request.title());
        assertThat(pathinfos).hasSize(1);
    }

    @Test
    @DisplayName("펀딩 생성 - 최소 인원 초과 예외")
    void createFunding_whenMinParticipantsExceedsCapacity_throwsException() {
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                70,
                TripType.ONE_WAY,
                oneWayRoute()
        );

        assertThatThrownBy(() ->
                fundingService.createFunding(member.getMemberId(), request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_MIN_INVALID);
    }

    @Test
    @DisplayName("왕복 펀딩 생성 성공")
    void createRoundFunding_success() {
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        assertThat(findPathinfos(response.fundingId())).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 펀딩 생성 예외")
    void createFunding_whenMemberDoesNotExist_throwsException() {
        assertThatThrownBy(() ->
                fundingService.createFunding(999L, oneWayCreateRequest())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("왕복 펀딩 - 복귀 시간 없음 예외")
    void createFunding_whenRoundTripHasNoReturnTime_throwsException() {
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.SEOUL_A)
        );

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        FundingDetailResponse result = fundingService.getFunding(response.fundingId());

        assertThat(result.fundingId()).isEqualTo(response.fundingId());
        assertThat(result.title()).isEqualTo("Football Match Bus");
        assertThat(result.pathinfos()).hasSize(1);
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 존재하지 않는 펀딩 예외")
    void getFunding_whenFundingDoesNotExist_throwsException() {
        assertThatThrownBy(() -> fundingService.getFunding(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
    }

    @Test
    @DisplayName("펀딩 목록 조회 성공")
    void getFundingList_success() {
        Member member = saveMember();
        fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(
                        emptyCondition(),
                        PageRequest.of(0, 10)
                );

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("펀딩 전체 수정 성공")
    void updateFunding_success() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        Funding funding = findFunding(response.fundingId());
        assertThat(funding.getTitle()).isEqualTo("Updated Title");
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_25);
    }

    @Test
    @DisplayName("존재하지 않는 펀딩 수정 예외")
    void updateFunding_whenFundingDoesNotExist_throwsException() {
        Member member = saveMember();

        assertThatThrownBy(() ->
                fundingService.updateFunding(member.getMemberId(), 999L, oneWayUpdateRequest())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
    }

    @Test
    @DisplayName("펀딩 취소 성공")
    void cancelFunding_success() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        Funding funding = findFunding(response.fundingId());
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CANCELLED);
    }

    @Test
    @DisplayName("이미 취소된 펀딩 취소 예외")
    void cancelFunding_whenAlreadyCancelled_throwsException() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        assertThatThrownBy(() ->
                fundingService.cancelFunding(member.getMemberId(), response.fundingId())
        )
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("편도->왕복 수정 오는 노선 생성")
    void updateFunding_fromOneWayToRound_createsReturnPathinfo() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                roundUpdateRequest()
        );

        assertThat(findPathinfos(response.fundingId())).hasSize(2);
    }

    @Test
    @DisplayName("왕복->편도 수정 오는 노선 취소")
    void updateFunding_fromRoundToOneWay_cancelsReturnPathinfo() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        assertThat(findPathinfos(response.fundingId()))
                .allMatch(pathinfo -> pathinfo.getStatus() == PathinfoStatus.CANCELLED);
    }

    @Test
    @DisplayName("왕복 노선 수정 성공")
    void updateFunding_roundPathinfo_success() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                500000,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        DEFAULT_RETURN_TIME,
                        "Gangnam Station",
                        Region.SEOUL_B,
                        "Seoul Stadium",
                        Region.SEOUL_A
                )
        );

        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

        assertThat(findPathinfos(response.fundingId()))
                .extracting(Pathinfo::getDepartureAddress)
                .contains("Gangnam Station");
    }

    @Test
    @DisplayName("생성 - 출발지역-도착지역 동일 예외")
    void createFunding_whenDepartureAndArrivalRegionsAreSame_throwsException() {
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.INCHEON)
        );

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                300000,
                route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.INCHEON)
        );

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
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ONE_WAY,
                route(LocalDateTime.now().plusDays(7), null, Region.INCHEON, Region.SEOUL_A)
        );

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
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 20, 5, 0),
                        Region.INCHEON,
                        Region.SEOUL_A
                )
        );

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
        Member member = saveMember();
        FundingCreateRequest request = createRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 22, 23, 0),
                        Region.INCHEON,
                        Region.SEOUL_A
                )
        );

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                300000,
                route(LocalDateTime.now().plusDays(7), null, Region.INCHEON, Region.SEOUL_A)
        );

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                500000,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 20, 5, 0),
                        Region.INCHEON,
                        Region.SEOUL_A
                )
        );

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());
        FundingUpdateRequest request = updateRequest(
                BusType.BUS_45,
                20,
                TripType.ROUND,
                500000,
                route(
                        DEFAULT_DEPARTURE_TIME,
                        LocalDateTime.of(2027, 6, 22, 23, 0),
                        Region.INCHEON,
                        Region.SEOUL_A
                )
        );

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
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

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
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

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
                500000,
                roundRoute()
        );

        fundingService.updateFunding(member.getMemberId(), response.fundingId(), request);

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
    @DisplayName("생성 - 버스타입에 따라 최대 인원 설정")
    void createFunding_setsMaxParticipantsFromBusType() {
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        Funding funding = findFunding(response.fundingId());
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_45);
        assertThat(funding.getMaxParticipants()).isEqualTo(BusType.BUS_45.getCapacity());
    }

    @Test
    @DisplayName("수정 - 버스타입 변경 시 최대 인원 변경")
    void updateFunding_whenBusTypeChanges_updatesMaxParticipants() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        Funding fundingBefore = findFunding(response.fundingId());
        assertThat(fundingBefore.getBusType()).isEqualTo(BusType.BUS_45);
        assertThat(fundingBefore.getMaxParticipants()).isEqualTo(BusType.BUS_45.getCapacity());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        Funding fundingAfter = findFunding(response.fundingId());
        assertThat(fundingAfter.getBusType()).isEqualTo(BusType.BUS_25);
        assertThat(fundingAfter.getMaxParticipants()).isEqualTo(BusType.BUS_25.getCapacity());
    }

    @Test
    @DisplayName("방장이 아닌 인원이 취소 시 예외")
    void cancelFunding_whenMemberIsNotHost_throwsException() {
        Member host = saveMember();
        Member other = saveOtherMember();
        FundingCreateResponse response =
                fundingService.createFunding(host.getMemberId(), roundCreateRequest());

        Funding funding = findFunding(response.fundingId());

        assertThatThrownBy(() ->
                fundingService.cancelFunding(other.getMemberId(), funding.getFundingId())
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(e ->
                        assertThat(((BusinessException) e).getErrorCode())
                                .isEqualTo(ErrorCode.FUNDING_FORBIDDEN)
                );
    }

    @Test
    @DisplayName("기본 목록에서 취소 펀딩 제외")
    void getFundingList_defaultStatusExcludesCancelledFunding() {
        Member member = saveMember();
        FundingCreateResponse active =
                fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());
        FundingCreateResponse cancelled =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.cancelFunding(member.getMemberId(), cancelled.fundingId());

        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(emptyCondition(), PageRequest.of(0, 10));

        assertThat(result.content())
                .extracting(FundingListResponse::fundingId)
                .containsExactly(active.fundingId());
    }

    @Test
    @DisplayName("취소 상태 목록에서 추가 시 취소된 노선 포함")
    void getFundingList_statusFilterIncludesCancelledPathinfo() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

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

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).fundingId()).isEqualTo(response.fundingId());
        assertThat(result.content().get(0).departureAddress()).isNotNull();
        assertThat(result.content().get(0).arrivalAddress()).isNotNull();
        assertThat(result.content().get(0).departureTime()).isNotNull();
    }

    @Test
    @DisplayName("펀딩 목록 - 상태 필터 동작")
    void getFundingList_filtersByRegionDateAndStatus() {
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
                                        Region.SEOUL_B,
                                        Region.INCHEON
                                )
                        )
                );

        fundingService.createFunding(member.getMemberId(), oneWayCreateRequest());

        PageResponse<FundingListResponse> result =
                fundingService.getFundingList(
                        new FundingSearchCondition(
                                List.of(FundingStatus.RECRUITING),
                                LocalDate.of(2027, 6, 21),
                                Region.SEOUL_B,
                                Region.INCHEON
                        ),
                        PageRequest.of(0, 10)
                );

        assertThat(result.content())
                .extracting(FundingListResponse::fundingId)
                .containsExactly(expected.fundingId());
    }

    @Test
    @DisplayName("수정 - 왕복->편도->왕복 오는 노선 재활성화")
    void updateFunding_roundToOneWayToRound_reactivatesReturnPathinfo() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );

        Pathinfo cancelledReturn = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(cancelledReturn.getStatus()).isEqualTo(PathinfoStatus.CANCELLED);

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                roundUpdateRequest()
        );

        Pathinfo returned = findPathinfo(response.fundingId(), Direction.RETURN);
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(pathinfoService.getPathinfoResponses(response.fundingId())).hasSize(2);
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 취소된 펀딩 상세 조회 시 최종 편도 기준 노선 조회")
    void getFunding_cancelledOneWayDetail_usesFinalTripType() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        fundingService.updateFunding(
                member.getMemberId(),
                response.fundingId(),
                oneWayUpdateRequest()
        );
        fundingService.cancelFunding(member.getMemberId(), response.fundingId());

        FundingDetailResponse result = fundingService.getFunding(response.fundingId());

        assertThat(result.tripType()).isEqualTo(TripType.ONE_WAY);
        assertThat(result.pathinfos()).hasSize(1);
        assertThat(result.pathinfos().get(0).direction()).isEqualTo(Direction.OUTBOUND);
    }

    @Test
    @DisplayName("펀딩 상세 조회 - 실패한 펀딩 상세 조회 시 최종 편도 기준 노선 조회")
    void getFunding_failedRoundDetail_usesFinalTripType() {
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(member.getMemberId(), roundCreateRequest());

        Funding funding = findFunding(response.fundingId());
        ReflectionTestUtils.setField(funding, "status", FundingStatus.FAILED);
        pathinfoService.cancelPathinfos(response.fundingId());

        FundingDetailResponse result = fundingService.getFunding(response.fundingId());

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
        return new FundingCreateRequest(
                "Football Match Bus",
                "Ride together",
                busType,
                minParticipants,
                tripType,
                500000,
                route
        );
    }

    private FundingUpdateRequest oneWayUpdateRequest() {
        return updateRequest(
                BusType.BUS_25,
                10,
                TripType.ONE_WAY,
                300000,
                oneWayRoute()
        );
    }

    private FundingUpdateRequest roundUpdateRequest() {
        return updateRequest(
                BusType.BUS_25,
                10,
                TripType.ROUND,
                300000,
                roundRoute()
        );
    }

    private FundingUpdateRequest updateRequest(
            BusType busType,
            int minParticipants,
            TripType tripType,
            int totalPrice,
            RouteRequest route
    ) {
        return new FundingUpdateRequest(
                "Updated Title",
                "Updated Content",
                busType,
                minParticipants,
                tripType,
                totalPrice,
                route
        );
    }

    private RouteRequest oneWayRoute() {
        return route(DEFAULT_DEPARTURE_TIME, null, Region.INCHEON, Region.SEOUL_A);
    }

    private RouteRequest roundRoute() {
        return route(DEFAULT_DEPARTURE_TIME, DEFAULT_RETURN_TIME, Region.INCHEON, Region.SEOUL_A);
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

    private Member saveMember() {
        Member member = Member.builder()
                .email("test@test.com")
                .password("1234")
                .name("test")
                .nickname("test")
                .phoneNumber("01012341234")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return memberRepository.save(member);
    }

    private Member saveOtherMember() {
        Member member = Member.builder()
                .email("test2@test.com")
                .password("1234")
                .name("other")
                .nickname("other")
                .phoneNumber("01012345678")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return memberRepository.save(member);
    }
}
