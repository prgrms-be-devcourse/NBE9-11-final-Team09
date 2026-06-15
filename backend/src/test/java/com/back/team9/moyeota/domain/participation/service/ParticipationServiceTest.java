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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.any;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    // ==================== createParticipation 테스트 ====================

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
}
