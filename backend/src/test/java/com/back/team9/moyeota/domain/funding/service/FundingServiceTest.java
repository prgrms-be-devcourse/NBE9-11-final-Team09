package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.dto.*;
import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoCreateRequest;
import com.back.team9.moyeota.domain.pathinfo.dto.PathInfoUpdateRequest;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathInfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathInfoRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class FundingServiceTest {
    @Autowired
    private FundingService fundingService;
    @Autowired
    private FundingRepository fundingRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PathInfoRepository pathInfoRepository;

    @Test
    void createFunding_편도펀딩_생성성공() {
        // Given
        Member member = saveMember();
        FundingCreateRequest request = createOneWayRequest();

        // When
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), request);

        // Then
        Funding funding = fundingRepository.findById(response.fundingId()).orElseThrow();

        assertThat(funding.getTitle()).isEqualTo(request.title());
        assertThat(pathInfoRepository.findByFunding_FundingId(funding.getFundingId())).hasSize(1);
    }

    @Test
    void createFunding_최소인원초과_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        70,
                        TripType.ONE_WAY,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.of(2027, 6, 20, 8, 0),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_MIN_INVALID);
    }

    @Test
    void createFunding_총금액0원_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        0,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.of(2027, 6, 20, 8, 0),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOTAL_PRICE);
    }

    @Test
    void createFunding_왕복노선_생성성공() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request = createRoundRequest();

        // When
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), request);

        // Then
        assertThat(pathInfoRepository.findByFunding_FundingId(response.fundingId())).hasSize(2);
    }

    @Test
    void createFunding_존재하지않는회원_예외발생() {

        // Given
        FundingCreateRequest request = createOneWayRequest();

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void createFunding_편도인데RETURN노선인경우_예외발생() {

        // Given
        Member member = saveMember();

        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.of(2027, 6, 20, 8, 0),
                                        "서울",
                                        Region.SEOUL_A,
                                        "인천",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    @Test
    void createFunding_왕복인데노선이한개인경우_예외발생() {

        // Given
        Member member = saveMember();

        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.of(2027, 6, 20, 8, 0),
                                        "서울",
                                        Region.SEOUL_A,
                                        "인천",
                                        Region.INCHEON,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PATH_CONFIGURATION);
    }

    @Test
    void getFunding_조회성공() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createOneWayRequest());

        // When
        FundingDetailResponse result = fundingService.getFunding(response.fundingId());

        // Then
        assertThat(result.fundingId()).isEqualTo(response.fundingId());
        assertThat(result.title()).isEqualTo("축구 경기 버스");
        assertThat(result.pathInfos()).hasSize(1);
    }

    @Test
    void getFunding_존재하지않는펀딩_예외발생() {

        assertThatThrownBy(() -> fundingService.getFunding(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
    }

    @Test
    void getFundingList_조회성공() {

        // Given
        Member member = saveMember();
        fundingService.createFunding(member.getMemberId(), createOneWayRequest());
        fundingService.createFunding(member.getMemberId(), createRoundRequest());

        // When
        List<FundingListResponse> result = fundingService.getFundingList();

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void updateFunding_전체수정성공() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createOneWayRequest());

        // When
        fundingService.updateFunding(response.fundingId(), createUpdateRequest());

        // Then
        Funding funding = fundingRepository.findById(response.fundingId()).orElseThrow();
        assertThat(funding.getTitle()).isEqualTo("수정 제목");
        assertThat(funding.getBusType()).isEqualTo(BusType.BUS_25);
    }

    @Test
    void updateFunding_존재하지않는펀딩_예외발생() {

        assertThatThrownBy(() -> fundingService.updateFunding(999L, createUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
    }

    @Test
    void cancelFunding_취소성공() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createOneWayRequest());

        // When
        fundingService.cancelFunding(response.fundingId());

        // Then
        Funding funding = fundingRepository.findById(response.fundingId()).orElseThrow();
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CANCELLED);
    }

    @Test
    void cancelFunding_이미취소된펀딩_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createOneWayRequest());
        fundingService.cancelFunding(response.fundingId());

        // When & Then
        assertThatThrownBy(() -> fundingService.cancelFunding(response.fundingId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_ALREADY_CANCELLED);
    }

    @Test
    void updateFunding_편도에서왕복으로변경_수정성공() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createOneWayRequest()
                );

        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정 제목",
                        "수정 내용",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        500000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(
                                                2027, 6, 20, 8, 0
                                        ),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(
                                                2027, 6, 20, 23, 0
                                        ),
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        "인천역",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        // When
        fundingService.updateFunding(
                response.fundingId(),
                request
        );

        // Then
        assertThat(
                pathInfoRepository.findByFunding_FundingId(
                        response.fundingId()
                )
        ).hasSize(2);
    }

    @Test
    void updateFunding_왕복에서편도로변경_수정성공() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRoundRequest()
                );

        FundingUpdateRequest request =
                createUpdateRequest();

        // When
        fundingService.updateFunding(
                response.fundingId(),
                request
        );

        // Then
        assertThat(
                pathInfoRepository.findByFunding_FundingId(
                        response.fundingId()
                )
        ).hasSize(1);

        assertThat(
                pathInfoRepository.findByFunding_FundingId(
                                response.fundingId()
                        )
                        .getFirst()
                        .getDirection()
        ).isEqualTo(
                Direction.OUTBOUND
        );
    }

    @Test
    void cancelFunding_노선상태도취소성공() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createRoundRequest());

        // When
        fundingService.cancelFunding(response.fundingId());

        // Then
        assertThat(pathInfoRepository.findByFunding_FundingId(response.fundingId()))
                .allMatch(pathInfo -> pathInfo.getStatus() == PathInfoStatus.CANCELLED);
    }

    @Test
    void updateFunding_왕복노선정보수정_수정성공() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createRoundRequest());
        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정",
                        "수정",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        500000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(2027, 6, 21, 10, 0),
                                        "강남역",
                                        Region.SEOUL_B,
                                        "잠실",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(2027, 6, 21, 23, 0),
                                        "잠실",
                                        Region.SEOUL_A,
                                        "강남역",
                                        Region.SEOUL_B,
                                        Direction.RETURN
                                )
                        )
                );

        // When
        fundingService.updateFunding(response.fundingId(), request);

        // Then
        List<PathInfo> paths = pathInfoRepository.findByFunding_FundingId(response.fundingId());
        assertThat(paths).extracting(PathInfo::getDepartureAddress).contains("강남역");
    }

    @Test
    void createFunding_출발지역과도착지역이같으면_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.of(2027, 6, 20, 8, 0),
                                        "강남역",
                                        Region.SEOUL_A,
                                        "잠실역",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_DEPARTURE_ARRIVAL);
    }

    @Test
    void updateFunding_출발지역과도착지역이같으면_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response = fundingService.createFunding(member.getMemberId(), createOneWayRequest());
        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정 제목",
                        "수정 내용",
                        BusType.BUS_25,
                        10,
                        TripType.ONE_WAY,
                        300000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(2027, 7, 1, 10, 0),
                                        "강남역",
                                        Region.SEOUL_A,
                                        "잠실종합운동장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.updateFunding(response.fundingId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_DEPARTURE_ARRIVAL);
    }

    @Test
    void createFunding_출발일이14일미만인경우_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ONE_WAY,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.now().plusDays(7),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTURE_DATE_TOO_SOON);
    }

    @Test
    void createFunding_복귀시간이출발시간보다빠른경우_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.now().plusDays(20).withHour(18),
                                        "인천",
                                        Region.INCHEON,
                                        "서울",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoCreateRequest(
                                        LocalDateTime.now().plusDays(20).withHour(12),
                                        "서울",
                                        Region.SEOUL_A,
                                        "인천",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_TIME_BEFORE_OUTBOUND);
    }

    @Test
    void createFunding_왕복날짜가다른경우_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateRequest request =
                new FundingCreateRequest(
                        "제목",
                        "내용",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        100000,
                        List.of(
                                new PathInfoCreateRequest(
                                        LocalDateTime.now().plusDays(20),
                                        "인천",
                                        Region.INCHEON,
                                        "서울",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoCreateRequest(
                                        LocalDateTime.now().plusDays(21),
                                        "서울",
                                        Region.SEOUL_A,
                                        "인천",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        assertThatThrownBy(() -> fundingService.createFunding(member.getMemberId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_DATE_MUST_MATCH_OUTBOUND);
    }

    @Test
    void updateFunding_출발일이14일미만인경우_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createOneWayRequest()
                );

        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정 제목",
                        "수정 내용",
                        BusType.BUS_25,
                        10,
                        TripType.ONE_WAY,
                        300000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.now().plusDays(7),
                                        "강남역",
                                        Region.SEOUL_A,
                                        "잠실종합운동장",
                                        Region.SEOUL_B,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.updateFunding(response.fundingId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPARTURE_DATE_TOO_SOON);
    }

    @Test
    void updateFunding_복귀시간이출발시간보다빠른경우_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRoundRequest()
                );

        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정 제목",
                        "수정 내용",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        500000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.now()
                                                .plusDays(20)
                                                .withHour(18),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoUpdateRequest(
                                        LocalDateTime.now()
                                                .plusDays(20)
                                                .withHour(12),
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        "인천역",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.updateFunding(response.fundingId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_TIME_BEFORE_OUTBOUND);
    }

    @Test
    void updateFunding_왕복날짜가다른경우_예외발생() {

        // Given
        Member member = saveMember();
        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRoundRequest()
                );
        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정 제목",
                        "수정 내용",
                        BusType.BUS_45,
                        20,
                        TripType.ROUND,
                        500000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.now()
                                                .plusDays(20),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoUpdateRequest(
                                        LocalDateTime.now()
                                                .plusDays(21),
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        "인천역",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        // When & Then
        assertThatThrownBy(() -> fundingService.updateFunding(response.fundingId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RETURN_DATE_MUST_MATCH_OUTBOUND);
    }

    @Test
    void createFunding_펀딩과모든노선의버스타입이일치한다() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRoundRequest()
                );

        // When
        Funding funding =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        List<PathInfo> pathInfos =
                pathInfoRepository.findByFunding_FundingId(
                        response.fundingId()
                );

        // Then
        assertThat(pathInfos)
                .hasSize(2);

        assertThat(
                funding.getBusType()
        ).isEqualTo(
                BusType.BUS_45
        );

        assertThat(pathInfos)
                .extracting(PathInfo::getBusType)
                .containsOnly(
                        funding.getBusType()
                );
    }

    @Test
    void updateFunding_펀딩과모든노선의버스타입이일치한다() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRoundRequest()
                );

        // When
        fundingService.updateFunding(
                response.fundingId(),
                createUpdateRequest()
        );

        Funding funding =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        List<PathInfo> pathInfos =
                pathInfoRepository.findByFunding_FundingId(
                        response.fundingId()
                );

        // Then
        assertThat(
                funding.getBusType()
        ).isEqualTo(
                BusType.BUS_25
        );

        assertThat(pathInfos)
                .extracting(PathInfo::getBusType)
                .containsOnly(
                        BusType.BUS_25
                );
    }

    @Test
    void updateFunding_버스타입만변경시_모든노선버스타입도변경된다() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createRoundRequest()
                );

        Funding fundingBefore =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        assertThat(fundingBefore.getBusType())
                .isEqualTo(BusType.BUS_45);

        assertThat(
                pathInfoRepository.findByFunding_FundingId(
                        response.fundingId()
                )
        )
                .extracting(PathInfo::getBusType)
                .containsOnly(BusType.BUS_45);

        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "축구 경기 버스",
                        "같이 갑시다",
                        BusType.BUS_25, // 버스타입만 변경
                        20,
                        TripType.ROUND,
                        500000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(
                                                2027, 6, 20, 8, 0
                                        ),
                                        "인천역",
                                        Region.INCHEON,
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        Direction.OUTBOUND
                                ),
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(
                                                2027, 6, 20, 23, 0
                                        ),
                                        "서울월드컵경기장",
                                        Region.SEOUL_A,
                                        "인천역",
                                        Region.INCHEON,
                                        Direction.RETURN
                                )
                        )
                );

        // When
        fundingService.updateFunding(
                response.fundingId(),
                request
        );

        // Then
        Funding fundingAfter =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        List<PathInfo> pathInfos =
                pathInfoRepository.findByFunding_FundingId(
                        response.fundingId()
                );

        assertThat(fundingAfter.getBusType())
                .isEqualTo(BusType.BUS_25);

        assertThat(pathInfos)
                .hasSize(2);

        assertThat(pathInfos)
                .extracting(PathInfo::getBusType)
                .containsOnly(BusType.BUS_25);
        assertThat(pathInfos.get(0).getBusType())
                .isEqualTo(fundingAfter.getBusType());

        assertThat(pathInfos.get(1).getBusType())
                .isEqualTo(fundingAfter.getBusType());
    }

    @Test
    void createFunding_버스타입선택시_최대인원이자동설정된다() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createOneWayRequest()
                );

        // When
        Funding funding =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        // Then
        assertThat(funding.getBusType())
                .isEqualTo(BusType.BUS_45);

        assertThat(funding.getMaxParticipants())
                .isEqualTo(
                        BusType.BUS_45.getCapacity()
                );
    }

    @Test
    void updateFunding_버스타입변경시_최대인원이자동변경된다() {

        // Given
        Member member = saveMember();

        FundingCreateResponse response =
                fundingService.createFunding(
                        member.getMemberId(),
                        createOneWayRequest()
                );

        Funding fundingBefore =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        assertThat(fundingBefore.getBusType())
                .isEqualTo(BusType.BUS_45);

        assertThat(fundingBefore.getMaxParticipants())
                .isEqualTo(
                        BusType.BUS_45.getCapacity()
                );

        FundingUpdateRequest request =
                new FundingUpdateRequest(
                        "수정 제목",
                        "수정 내용",
                        BusType.BUS_25,
                        10,
                        TripType.ONE_WAY,
                        300000,
                        List.of(
                                new PathInfoUpdateRequest(
                                        LocalDateTime.of(
                                                2026, 7, 1, 10, 0
                                        ),
                                        "강남역",
                                        Region.SEOUL_A,
                                        "잠실종합운동장",
                                        Region.SEOUL_B,
                                        Direction.OUTBOUND
                                )
                        )
                );

        // When
        fundingService.updateFunding(
                response.fundingId(),
                request
        );

        // Then
        Funding fundingAfter =
                fundingRepository.findById(
                        response.fundingId()
                ).orElseThrow();

        assertThat(fundingAfter.getBusType())
                .isEqualTo(BusType.BUS_25);

        assertThat(fundingAfter.getMaxParticipants())
                .isEqualTo(
                        BusType.BUS_25.getCapacity()
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
                List.of(
                        new PathInfoCreateRequest(
                                LocalDateTime.of(2027, 6, 20, 8, 0),
                                "인천역",
                                Region.INCHEON,
                                "서울월드컵경기장",
                                Region.SEOUL_A,
                                Direction.OUTBOUND
                        )
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
                List.of(
                        new PathInfoCreateRequest(
                                LocalDateTime.of(2027, 6, 20, 8, 0),
                                "인천역",
                                Region.INCHEON,
                                "서울월드컵경기장",
                                Region.SEOUL_A,
                                Direction.OUTBOUND
                        ),
                        new PathInfoCreateRequest(
                                LocalDateTime.of(2027, 6, 20, 23, 0),
                                "서울월드컵경기장",
                                Region.SEOUL_A,
                                "인천역",
                                Region.INCHEON,
                                Direction.RETURN
                        )
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
                List.of(
                        new PathInfoUpdateRequest(
                                LocalDateTime.of(2027, 6, 1, 10, 0),
                                "강남역",
                                Region.SEOUL_A,
                                "잠실종합운동장",
                                Region.SEOUL_B,
                                Direction.OUTBOUND
                        )
                )
        );
    }

    private Member saveMember() {
        Member member = Member.builder()
                .email("test@test.com")
                .password("1234")
                .name("테스트")
                .nickname("테스터")
                .phoneNumber("01012341234")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return memberRepository.save(member);
    }
}
