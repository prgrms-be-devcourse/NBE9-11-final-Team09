package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.domain.seat.service.SeatRedisService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatRedisService seatRedisService;

    @InjectMocks
    private ParticipationService participationService;

    @Test
    @DisplayName("참여 신청 - 편도 펀딩 정상 신청 성공")
    void createParticipation_편도정상신청_성공() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 100L;

        // 요청 DTO - 편도라 returnSeatId는 null
        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        // Funding Mock - 편도(ONE_WAY), 모집중(RECRUITING), 정원 10명
        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);
        given(funding.getTripType()).willReturn(TripType.ONE_WAY);

        // Member Mock
        Member member = mock(Member.class);

        // outboundSeat과 연결된 Pathinfo Mock - 이 펀딩의 OUTBOUND 노선
        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(funding);
        given(outboundPathinfo.getDirection()).willReturn(Direction.OUTBOUND);

        // outboundSeat Mock - 예약 가능한 상태
        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getSeatId()).willReturn(outboundSeatId);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        // Repository Mock 동작 정의
        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberId(fundingId, memberId))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndStatus(fundingId, ParticipationStatus.ACTIVE))
                .willReturn(0L);
        given(seatRepository.findById(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When
        ParticipationResponse response = participationService.createParticipation(memberId, request);

        // Then
        assertThat(response.status()).isEqualTo(ParticipationStatus.ACTIVE);
        assertThat(response.finalAmount()).isEqualTo(0);
        assertThat(response.outboundSeatId()).isEqualTo(outboundSeatId);
        assertThat(response.returnSeatId()).isNull();

        // 좌석이 BOOKED로 확정됐는지, Redis HOLD가 해제됐는지 확인
        verify(outboundSeat).book(any());
        verify(seatRedisService).releaseSeat(outboundSeatId, memberId);
        verify(participationRepository).save(any());
    }


    @Test
    @DisplayName("참여 신청 - 왕복 펀딩 정상 신청 성공")
    void createParticipation_왕복정상신청_성공() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 100L;
        Long returnSeatId = 200L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                returnSeatId
        );

        // Funding Mock - 왕복(ROUND), 모집중(RECRUITING), 정원 10명
        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);
        given(funding.getTripType()).willReturn(TripType.ROUND);

        // Member Mock
        Member member = mock(Member.class);

        // outboundSeat - OUTBOUND 노선
        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(funding);
        given(outboundPathinfo.getDirection()).willReturn(Direction.OUTBOUND);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getSeatId()).willReturn(outboundSeatId);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        // returnSeat - RETURN 노선
        Pathinfo returnPathinfo = mock(Pathinfo.class);
        given(returnPathinfo.getFunding()).willReturn(funding);
        given(returnPathinfo.getDirection()).willReturn(Direction.RETURN);

        Seat returnSeat = mock(Seat.class);
        given(returnSeat.getSeatId()).willReturn(returnSeatId);
        given(returnSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(returnSeat.getPathinfo()).willReturn(returnPathinfo);

        // Repository Mock 동작 정의
        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberId(fundingId, memberId))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndStatus(fundingId, ParticipationStatus.ACTIVE))
                .willReturn(0L);
        given(seatRepository.findById(outboundSeatId)).willReturn(Optional.of(outboundSeat));
        given(seatRepository.findById(returnSeatId)).willReturn(Optional.of(returnSeat));

        // When
        ParticipationResponse response = participationService.createParticipation(memberId, request);

        // Then
        assertThat(response.status()).isEqualTo(ParticipationStatus.ACTIVE);
        assertThat(response.outboundSeatId()).isEqualTo(outboundSeatId);
        assertThat(response.returnSeatId()).isEqualTo(returnSeatId);

        // 가는편/오는편 좌석 모두 BOOKED 확정, Redis HOLD 모두 해제
        verify(outboundSeat).book(any());
        verify(returnSeat).book(any());
        verify(seatRedisService).releaseSeat(outboundSeatId, memberId);
        verify(seatRedisService).releaseSeat(returnSeatId, memberId);
        verify(participationRepository).save(any());
    }

    @Test
    @DisplayName("참여 신청 - 존재하지 않는 펀딩 FUNDING_NOT_FOUND 예외 발생")
    void createParticipation_존재하지않는펀딩_FUNDING_NOT_FOUND예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 999L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                100L,
                null
        );

        given(fundingRepository.findById(fundingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_NOT_FOUND));

        // 펀딩이 없으니 그 이후 로직(좌석 조회 등)은 호출되면 안 됨
        verify(seatRepository, never()).findById(any());
    }

    @Test
    @DisplayName("참여 신청 - 존재하지 않는 회원 USER_NOT_FOUND 예외 발생")
    void createParticipation_존재하지않는회원_USER_NOT_FOUND예외() {
        // Given
        Long memberId = 999L;
        Long fundingId = 10L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                100L,
                null
        );

        Funding funding = mock(Funding.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(seatRepository, never()).findById(any());
    }

    @Test
    @DisplayName("참여 신청 - 취소된 펀딩 FUNDING_CANCELLED 예외 발생")
    void createParticipation_취소된펀딩_FUNDING_CANCELLED예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                100L,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getStatus()).willReturn(FundingStatus.CANCELLED);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_CANCELLED));

        verify(seatRepository, never()).findById(any());
    }

    @Test
    @DisplayName("참여 신청 - 모집 종료된 펀딩(COMPLETED) FUNDING_RECRUITMENT_CLOSED 예외 발생")
    void createParticipation_모집종료된펀딩_FUNDING_RECRUITMENT_CLOSED예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                100L,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getStatus()).willReturn(FundingStatus.COMPLETED);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_RECRUITMENT_CLOSED));

        verify(seatRepository, never()).findById(any());
    }


}
