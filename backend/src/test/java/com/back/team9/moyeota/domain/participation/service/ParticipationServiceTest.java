package com.back.team9.moyeota.domain.participation.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.participation.dto.ParticipationCreateRequest;
import com.back.team9.moyeota.domain.participation.dto.ParticipationListResponse;
import com.back.team9.moyeota.domain.participation.dto.ParticipationResponse;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.domain.seat.entity.Seat;
import com.back.team9.moyeota.domain.seat.entity.SeatStatus;
import com.back.team9.moyeota.domain.seat.repository.SeatRepository;
import com.back.team9.moyeota.domain.seat.service.SeatRedisService;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.domain.participation.event.ParticipationCancelledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2027, 6, 20, 9, 0);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW.atZone(ZONE).toInstant(), ZONE);

    @Mock
    private ParticipationRepository participationRepository;

    private ParticipationService participationService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatRedisService seatRedisService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.back.team9.moyeota.domain.notification.service.NotificationService notificationService;


    @BeforeEach
    void setUp() {
        participationService = serviceWithClock(FIXED_CLOCK);
    }

    private ParticipationService serviceWithClock(Clock clock) {
        return new ParticipationService(
                participationRepository,
                paymentRepository,
                fundingRepository,
                memberRepository,
                seatRepository,
                seatRedisService,
                eventPublisher,
                notificationService,
                clock
        );
    }

    private Clock clockAt(LocalDateTime dateTime) {
        return Clock.fixed(dateTime.atZone(ZONE).toInstant(), ZONE);
    }

    @Test
    @DisplayName("참여 신청 - 편도 펀딩 정상 신청 성공")
    void createParticipation_편도정상신청_성공() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 100L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);
        given(funding.getTripType()).willReturn(TripType.ONE_WAY);

        Member member = mock(Member.class);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(funding);
        given(outboundPathinfo.getDirection()).willReturn(Direction.OUTBOUND);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getSeatId()).willReturn(outboundSeatId);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(
                fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(participationRepository.findByFunding_FundingIdAndMember_MemberId(fundingId, memberId))
                .willReturn(Optional.empty());
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When
        ParticipationResponse response = participationService.createParticipation(memberId, request);

        // Then
        assertThat(response.status()).isEqualTo(ParticipationStatus.ACTIVE);
        assertThat(response.finalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.outboundSeatId()).isEqualTo(outboundSeatId);
        assertThat(response.returnSeatId()).isNull();

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

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);
        given(funding.getTripType()).willReturn(TripType.ROUND);

        Member member = mock(Member.class);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(funding);
        given(outboundPathinfo.getDirection()).willReturn(Direction.OUTBOUND);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getSeatId()).willReturn(outboundSeatId);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Pathinfo returnPathinfo = mock(Pathinfo.class);
        given(returnPathinfo.getFunding()).willReturn(funding);
        given(returnPathinfo.getDirection()).willReturn(Direction.RETURN);

        Seat returnSeat = mock(Seat.class);
        given(returnSeat.getSeatId()).willReturn(returnSeatId);
        given(returnSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(returnSeat.getPathinfo()).willReturn(returnPathinfo);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(participationRepository.findByFunding_FundingIdAndMember_MemberId(fundingId, memberId))
                .willReturn(Optional.empty());
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));
        given(seatRepository.findByIdWithPathinfoAndFunding(returnSeatId)).willReturn(Optional.of(returnSeat));

        // When
        ParticipationResponse response = participationService.createParticipation(memberId, request);

        // Then
        assertThat(response.status()).isEqualTo(ParticipationStatus.ACTIVE);
        assertThat(response.outboundSeatId()).isEqualTo(outboundSeatId);
        assertThat(response.returnSeatId()).isEqualTo(returnSeatId);

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

        verify(seatRepository, never()).findByIdWithPathinfoAndFunding(any());
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

        verify(seatRepository, never()).findByIdWithPathinfoAndFunding(any());
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

        verify(seatRepository, never()).findByIdWithPathinfoAndFunding(any());
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

        verify(seatRepository, never()).findByIdWithPathinfoAndFunding(any());
    }


    @Test
    @DisplayName("참여 신청 - 이미 참여 중인 펀딩 DUPLICATE_PARTICIPATION 예외 발생")
    void createParticipation_이미참여중_DUPLICATE_PARTICIPATION예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                100L,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_PARTICIPATION));

        verify(seatRepository, never()).findByIdWithPathinfoAndFunding(any());
    }

    @Test
    @DisplayName("참여 신청 - 정원 초과 FUNDING_RECRUITMENT_CLOSED 예외 발생")
    void createParticipation_정원초과_FUNDING_RECRUITMENT_CLOSED예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                100L,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(10L);

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_RECRUITMENT_CLOSED));

        verify(seatRepository, never()).findByIdWithPathinfoAndFunding(any());
    }


    @Test
    @DisplayName("참여 신청 - 존재하지 않는 좌석 SEAT_NOT_FOUND 예외 발생")
    void createParticipation_존재하지않는좌석_SEAT_NOT_FOUND예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 999L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_NOT_FOUND));
    }

    @Test
    @DisplayName("참여 신청 - 이미 BOOKED된 좌석 SEAT_ALREADY_OCCUPIED 예외 발생")
    void createParticipation_이미BOOKED된좌석_SEAT_ALREADY_OCCUPIED예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 100L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);

        Member member = mock(Member.class);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.BOOKED);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_ALREADY_OCCUPIED));
    }

    @Test
    @DisplayName("참여 신청 - 다른 펀딩의 좌석 SEAT_NOT_IN_PATH 예외 발생")
    void createParticipation_다른펀딩좌석_SEAT_NOT_IN_PATH예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long otherFundingId = 20L;
        Long outboundSeatId = 100L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);

        Funding otherFunding = mock(Funding.class);
        given(otherFunding.getFundingId()).willReturn(otherFundingId);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(otherFunding);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_NOT_IN_PATH));
    }

    @Test
    @DisplayName("참여 신청 - outbound 자리에 RETURN 노선 좌석 선택 SEAT_NOT_IN_PATH 예외 발생")
    void createParticipation_방향불일치좌석_SEAT_NOT_IN_PATH예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 100L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);

        Pathinfo wrongDirectionPathinfo = mock(Pathinfo.class);
        given(wrongDirectionPathinfo.getFunding()).willReturn(funding);
        given(wrongDirectionPathinfo.getDirection()).willReturn(Direction.RETURN);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(wrongDirectionPathinfo);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_NOT_IN_PATH));
    }

    @Test
    @DisplayName("참여 신청 - 왕복인데 returnSeatId 없음 ROUND_TRIP_SEAT_REQUIRED 예외 발생")
    void createParticipation_왕복인데오는편좌석없음_ROUND_TRIP_SEAT_REQUIRED예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 10L;
        Long outboundSeatId = 100L;

        ParticipationCreateRequest request = new ParticipationCreateRequest(
                fundingId,
                outboundSeatId,
                null
        );

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);
        given(funding.getTripType()).willReturn(TripType.ROUND);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(funding);
        given(outboundPathinfo.getDirection()).willReturn(Direction.OUTBOUND);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ROUND_TRIP_SEAT_REQUIRED));
    }

    @Test
    @DisplayName("참여 신청 - 편도인데 returnSeatId 있음 ONE_WAY_RETURN_SEAT_NOT_ALLOWED 예외 발생")
    void createParticipation_편도인데오는편좌석있음_ONE_WAY_RETURN_SEAT_NOT_ALLOWED예외() {
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

        Funding funding = mock(Funding.class);
        given(funding.getFundingId()).willReturn(fundingId);
        given(funding.getStatus()).willReturn(FundingStatus.RECRUITING);
        given(funding.getMaxParticipants()).willReturn(10);
        given(funding.getTripType()).willReturn(TripType.ONE_WAY);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getFunding()).willReturn(funding);
        given(outboundPathinfo.getDirection()).willReturn(Direction.OUTBOUND);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getStatus()).willReturn(SeatStatus.AVAILABLE);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Member member = mock(Member.class);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(participationRepository.existsByFunding_FundingIdAndMember_MemberIdAndPaymentStatusIn(
                eq(fundingId), eq(memberId), any()))
                .willReturn(false);
        given(participationRepository.countByFunding_FundingIdAndPaymentStatusIn(fundingId, List.of(ParticipationPaymentStatus.PENDING, ParticipationPaymentStatus.ACTIVE)))
                .willReturn(0L);
        given(seatRepository.findByIdWithPathinfoAndFunding(outboundSeatId)).willReturn(Optional.of(outboundSeat));

        // When & Then
        assertThatThrownBy(() -> participationService.createParticipation(memberId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ONE_WAY_RETURN_SEAT_NOT_ALLOWED));
    }

    @Test
    @DisplayName("참여 취소 - 본인 참여 내역 없음 PARTICIPATION_NOT_FOUND 예외 발생")
    void cancelParticipation_본인참여내역없음_PARTICIPATION_NOT_FOUND예외() {
        // Given
        Long memberId = 1L;
        Long participationId = 999L;

        given(participationRepository.findByParticipationIdAndMember_MemberId(participationId, memberId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participationService.cancelParticipation(memberId, participationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND));
    }

    @Test
    @DisplayName("참여 취소 - 이미 취소된 참여 ALREADY_CANCELED_PARTICIPATION 예외 발생")
    void cancelParticipation_이미취소된참여_ALREADY_CANCELED_PARTICIPATION예외() {
        // Given
        Long memberId = 1L;
        Long participationId = 100L;

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.CANCELED);

        given(participationRepository.findByParticipationIdAndMember_MemberId(participationId, memberId))
                .willReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> participationService.cancelParticipation(memberId, participationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CANCELED_PARTICIPATION));

        verify(participation, never()).cancel();
    }

    @Test
    @DisplayName("참여 취소 - 취소 마감(출발 7일 전 자정) 한참 지난 경우 PARTICIPATION_CANCEL_NOT_ALLOWED 예외 발생")
    void cancelParticipation_취소마감한참지남_PARTICIPATION_CANCEL_NOT_ALLOWED예외() {
        // Given
        LocalDateTime departureTime = FIXED_NOW.plusHours(12);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);

        given(participationRepository.findByParticipationIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> participationService.cancelParticipation(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED));

        verify(participation, never()).cancel();
    }

    @Test
    @DisplayName("참여 취소 - 환불 대상(10일 전 이전) 취소 성공, 이벤트 발행됨")
    void cancelParticipation_환불대상취소_이벤트발행성공() {
        // Given
        Long memberId = 1L;
        Long participationId = 100L;

        LocalDateTime departureTime = FIXED_NOW.plusDays(15);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(participationRepository.findByParticipationIdAndMember_MemberId(participationId, memberId))
                .willReturn(Optional.of(participation));

        // When
        participationService.cancelParticipation(memberId, participationId);

        // Then
        verify(participation).cancel();
        verify(outboundSeat).release();

        ArgumentCaptor<ParticipationCancelledEvent> captor =
                ArgumentCaptor.forClass(ParticipationCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().participationId()).isEqualTo(participationId);
    }

    @Test
    @DisplayName("참여 취소 - 10일~7일 사이 취소, 환불 없이 취소만 처리 (이벤트 미발행)")
    void cancelParticipation_환불기간지남_이벤트미발행() {
        // Given
        Long memberId = 1L;
        Long participationId = 100L;

        LocalDateTime departureTime = FIXED_NOW.plusDays(8);

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(participationRepository.findByParticipationIdAndMember_MemberId(participationId, memberId))
                .willReturn(Optional.of(participation));

        // When
        participationService.cancelParticipation(memberId, participationId);

        // Then
        verify(participation).cancel();
        verify(outboundSeat).release();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("참여 취소 - 취소 마감 시점 정각, 취소 허용 (경계값)")
    void cancelParticipation_취소마감시점정각_취소허용() {
        // Given
        LocalDateTime departureTime = LocalDateTime.of(2027, 7, 7, 8, 0);
        LocalDateTime cancelDeadline = departureTime.toLocalDate().minusDays(7).atStartOfDay();
        participationService = serviceWithClock(clockAt(cancelDeadline));

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(participationRepository.findByParticipationIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(participation));

        // When & Then
        participationService.cancelParticipation(1L, 100L);
        verify(participation).cancel();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("참여 취소 - 취소 마감 시점 1초 지남, 예외 발생 (경계값)")
    void cancelParticipation_취소마감1초지남_PARTICIPATION_CANCEL_NOT_ALLOWED예외() {
        // Given
        LocalDateTime departureTime = LocalDateTime.of(2027, 7, 7, 8, 0);
        LocalDateTime cancelDeadline = departureTime.toLocalDate().minusDays(7).atStartOfDay();
        participationService = serviceWithClock(clockAt(cancelDeadline.plusSeconds(1)));

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);

        given(participationRepository.findByParticipationIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> participationService.cancelParticipation(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED));

        verify(participation, never()).cancel();
    }

    @Test
    @DisplayName("참여 취소 - 환불 마감 시점 1초 전, 이벤트 발행됨 (경계값)")
    void cancelParticipation_환불마감1초전_이벤트발행() {
        // Given
        LocalDateTime departureTime = LocalDateTime.of(2027, 7, 7, 8, 0);
        LocalDateTime refundDeadline = departureTime.toLocalDate().minusDays(10).atStartOfDay();
        participationService = serviceWithClock(clockAt(refundDeadline.minusSeconds(1)));

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(participationRepository.findByParticipationIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(participation));

        // When
        participationService.cancelParticipation(1L, 100L);

        // Then
        verify(participation).cancel();

        ArgumentCaptor<ParticipationCancelledEvent> captor =
                ArgumentCaptor.forClass(ParticipationCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().participationId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("참여 취소 - 환불 마감 시점 정각, 이벤트 미발행 (경계값)")
    void cancelParticipation_환불마감시점정각_이벤트미발행() {
        // Given
        LocalDateTime departureTime = LocalDateTime.of(2027, 7, 7, 8, 0);
        LocalDateTime refundDeadline = departureTime.toLocalDate().minusDays(10).atStartOfDay();
        participationService = serviceWithClock(clockAt(refundDeadline));

        Pathinfo outboundPathinfo = mock(Pathinfo.class);
        given(outboundPathinfo.getDepartureTime()).willReturn(departureTime);

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getPathinfo()).willReturn(outboundPathinfo);

        Participation participation = mock(Participation.class);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(participationRepository.findByParticipationIdAndMember_MemberId(100L, 1L))
                .willReturn(Optional.of(participation));

        // When
        participationService.cancelParticipation(1L, 100L);

        // Then
        verify(participation).cancel();
        verify(eventPublisher, never()).publishEvent(any());
    }


    @Test
    @DisplayName("참여자 목록 조회 - 정상 조회 성공")
    void getParticipations_정상조회_목록반환() {
        // Given
        Long memberId = 1L; // 방장 ID
        Long fundingId = 10L;

        Funding funding = mock(Funding.class);
        Member hostMember = mock(Member.class);
        given(hostMember.getMemberId()).willReturn(memberId);
        given(funding.getMember()).willReturn(hostMember);

        Member participantMember = mock(Member.class);
        given(participantMember.getNickname()).willReturn("모여타요");

        Seat outboundSeat = mock(Seat.class);
        given(outboundSeat.getSeatNumber()).willReturn("1A");

        Participation participation = mock(Participation.class);
        given(participation.getParticipationId()).willReturn(100L);
        given(participation.getMember()).willReturn(participantMember);
        given(participation.getStatus()).willReturn(ParticipationStatus.ACTIVE);
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.ACTIVE);
        given(participation.getOutboundSeat()).willReturn(outboundSeat);
        given(participation.getReturnSeat()).willReturn(null);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
        given(participationRepository.findByFunding_FundingId(fundingId))
                .willReturn(List.of(participation));

        // When
        List<ParticipationListResponse> response =
                participationService.getParticipations(memberId, fundingId);

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).participationId()).isEqualTo(100L);
        assertThat(response.get(0).memberNickname()).isEqualTo("모여타요");
        assertThat(response.get(0).outboundSeatNumber()).isEqualTo("1A");
        assertThat(response.get(0).returnSeatNumber()).isNull();
    }

    @Test
    @DisplayName("참여자 목록 조회 - 존재하지 않는 펀딩 FUNDING_NOT_FOUND 예외 발생")
    void getParticipations_존재하지않는펀딩_FUNDING_NOT_FOUND예외() {
        // Given
        Long memberId = 1L;
        Long fundingId = 999L;

        given(fundingRepository.findById(fundingId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participationService.getParticipations(memberId, fundingId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_NOT_FOUND));
    }

    @Test
    @DisplayName("참여자 목록 조회 - 방장이 아닌 경우 FUNDING_FORBIDDEN 예외 발생")
    void getParticipations_방장아님_FUNDING_FORBIDDEN예외() {
        // Given
        Long requestMemberId = 2L;
        Long hostMemberId = 1L;
        Long fundingId = 10L;

        Funding funding = mock(Funding.class);
        Member hostMember = mock(Member.class);
        given(hostMember.getMemberId()).willReturn(hostMemberId);
        given(funding.getMember()).willReturn(hostMember);

        given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));

        // When & Then
        assertThatThrownBy(() -> participationService.getParticipations(requestMemberId, fundingId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FUNDING_FORBIDDEN));

        verify(participationRepository, never()).findByFunding_FundingId(any());
    }

    @Test
    @DisplayName("잔액 결제 완료 - ACTIVE 상태에서 COMPLETED로 정상 전환")
    void completeBalancePayment_ACTIVE상태_COMPLETED전환_성공() {
        // Given
        Long participationId = 100L;

        Participation participation = mock(Participation.class);
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.ACTIVE);

        given(participationRepository.findById(participationId))
                .willReturn(Optional.of(participation));

        // When
        participationService.completeBalancePayment(participationId);

        // Then
        verify(participation).completePayment();
    }

    @Test
    @DisplayName("잔액 결제 완료 - 이미 COMPLETED 상태면 멱등성 보장 (중복 처리 방지)")
    void completeBalancePayment_이미COMPLETED상태_멱등성보장() {
        // Given
        Long participationId = 100L;

        Participation participation = mock(Participation.class);
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.COMPLETED);

        given(participationRepository.findById(participationId))
                .willReturn(Optional.of(participation));

        // When
        participationService.completeBalancePayment(participationId);

        // Then
        verify(participation, never()).completePayment();
    }

    @Test
    @DisplayName("잔액 결제 완료 - PENDING 상태에서 시도 시 INVALID_PARTICIPATION_STATUS 예외 발생")
    void completeBalancePayment_PENDING상태_INVALID_PARTICIPATION_STATUS예외() {
        // Given
        Long participationId = 100L;

        Participation participation = mock(Participation.class);
        given(participation.getPaymentStatus()).willReturn(ParticipationPaymentStatus.PENDING);

        given(participationRepository.findById(participationId))
                .willReturn(Optional.of(participation));

        // When & Then
        assertThatThrownBy(() -> participationService.completeBalancePayment(participationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PARTICIPATION_STATUS));

        verify(participation, never()).completePayment();
    }

    @Test
    @DisplayName("잔액 결제 완료 - 존재하지 않는 참여 PARTICIPATION_NOT_FOUND 예외 발생")
    void completeBalancePayment_존재하지않는참여_PARTICIPATION_NOT_FOUND예외() {
        // Given
        Long participationId = 999L;

        given(participationRepository.findById(participationId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> participationService.completeBalancePayment(participationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPATION_NOT_FOUND));
    }

}
