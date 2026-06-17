package com.back.team9.moyeota.domain.funding.service;

import com.back.team9.moyeota.domain.funding.entity.BusType;
import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.funding.entity.TripType;
import com.back.team9.moyeota.domain.funding.repository.FundingRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.domain.participation.repository.ParticipationRepository;
import com.back.team9.moyeota.domain.pathinfo.entity.Direction;
import com.back.team9.moyeota.domain.pathinfo.entity.Pathinfo;
import com.back.team9.moyeota.domain.pathinfo.entity.PathinfoStatus;
import com.back.team9.moyeota.domain.pathinfo.entity.Region;
import com.back.team9.moyeota.domain.pathinfo.repository.PathinfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FundingTimelineProcessorUnitTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 6, 10);
    private static final LocalDateTime NOW =
            LocalDateTime.of(2027, 6, 20, 9, 0);

    @InjectMocks
    private FundingTimelineProcessor fundingTimelineProcessor;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private PathinfoRepository pathinfoRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Test
    @DisplayName("D-10 이전 모집 중 펀딩 - 최소 인원 이상이면 확정")
    void confirmOrFailFundings_whenActiveParticipantsMeetMinimum_confirmsFunding() {
        // Given
        Funding funding = funding(10L, FundingStatus.RECRUITING, 20);
        given(fundingRepository.findByStatusAndDepartureDateLessThanEqual(
                FundingStatus.RECRUITING,
                TODAY.plusDays(10)
        )).willReturn(List.of(funding));
        given(participationRepository.countByFundingIdsAndStatus(
                List.of(10L),
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participantCount(10L, 20L)));

        // When
        fundingTimelineProcessor.confirmOrFailFundings(TODAY);

        // Then
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("D-10 이전 모집 중 펀딩 - 최소 인원 미만이면 실패")
    void confirmOrFailFundings_whenActiveParticipantsAreBelowMinimum_failsFunding() {
        // Given
        Funding funding = funding(10L, FundingStatus.RECRUITING, 20);
        given(fundingRepository.findByStatusAndDepartureDateLessThanEqual(
                FundingStatus.RECRUITING,
                TODAY.plusDays(10)
        )).willReturn(List.of(funding));
        given(participationRepository.countByFundingIdsAndStatus(
                List.of(10L),
                ParticipationStatus.ACTIVE
        )).willReturn(List.of(participantCount(10L, 19L)));

        // When
        fundingTimelineProcessor.confirmOrFailFundings(TODAY);

        // Then
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.FAILED);
    }

    @Test
    @DisplayName("D-10 이전 모집 중 펀딩 - 참가자 수 조회 결과가 없으면 0명으로 보고 실패")
    void confirmOrFailFundings_whenParticipantCountDoesNotExist_failsFunding() {
        // Given
        Funding funding = funding(10L, FundingStatus.RECRUITING, 20);
        given(fundingRepository.findByStatusAndDepartureDateLessThanEqual(
                FundingStatus.RECRUITING,
                TODAY.plusDays(10)
        )).willReturn(List.of(funding));
        given(participationRepository.countByFundingIdsAndStatus(
                List.of(10L),
                ParticipationStatus.ACTIVE
        )).willReturn(List.of());

        // When
        fundingTimelineProcessor.confirmOrFailFundings(TODAY);

        // Then
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.FAILED);
    }

    @Test
    @DisplayName("D-10 이전 모집 중 펀딩 - 대상이 없으면 참가자 수를 조회하지 않는다")
    void confirmOrFailFundings_whenTargetFundingDoesNotExist_doesNotCountParticipants() {
        // Given
        given(fundingRepository.findByStatusAndDepartureDateLessThanEqual(
                FundingStatus.RECRUITING,
                TODAY.plusDays(10)
        )).willReturn(List.of());

        // When
        fundingTimelineProcessor.confirmOrFailFundings(TODAY);

        // Then
        verifyNoInteractions(participationRepository);
    }

    @Test
    @DisplayName("출발 시간이 지난 노선 - 모든 유효 노선이 완료되면 펀딩도 완료")
    void completePathinfosAndFundings_whenAllPathinfosAreCompleted_completesFunding() {
        // Given
        Funding funding = funding(10L, FundingStatus.CONFIRMED, 20);
        Pathinfo outbound = pathinfo(100L, funding, Direction.OUTBOUND, PathinfoStatus.PENDING);
        Pathinfo returned = pathinfo(101L, funding, Direction.RETURN, PathinfoStatus.COMPLETED);

        given(pathinfoRepository.findPathinfosWithFunding(
                PathinfoStatus.PENDING,
                NOW,
                FundingStatus.CONFIRMED
        )).willReturn(List.of(outbound));
        given(pathinfoRepository.findByFunding_FundingIdInAndStatusNot(
                List.of(10L),
                PathinfoStatus.CANCELLED
        )).willReturn(List.of(outbound, returned));

        // When
        fundingTimelineProcessor.completePathinfosAndFundings(NOW);

        // Then
        assertThat(outbound.getStatus()).isEqualTo(PathinfoStatus.COMPLETED);
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.COMPLETED);
    }

    @Test
    @DisplayName("출발 시간이 지난 노선 - 미완료 노선이 남아 있으면 펀딩은 완료하지 않는다")
    void completePathinfosAndFundings_whenPendingPathinfoRemains_doesNotCompleteFunding() {
        // Given
        Funding funding = funding(10L, FundingStatus.CONFIRMED, 20);
        Pathinfo outbound = pathinfo(100L, funding, Direction.OUTBOUND, PathinfoStatus.PENDING);
        Pathinfo returned = pathinfo(101L, funding, Direction.RETURN, PathinfoStatus.PENDING);

        given(pathinfoRepository.findPathinfosWithFunding(
                PathinfoStatus.PENDING,
                NOW,
                FundingStatus.CONFIRMED
        )).willReturn(List.of(outbound));
        given(pathinfoRepository.findByFunding_FundingIdInAndStatusNot(
                List.of(10L),
                PathinfoStatus.CANCELLED
        )).willReturn(List.of(outbound, returned));

        // When
        fundingTimelineProcessor.completePathinfosAndFundings(NOW);

        // Then
        assertThat(outbound.getStatus()).isEqualTo(PathinfoStatus.COMPLETED);
        assertThat(returned.getStatus()).isEqualTo(PathinfoStatus.PENDING);
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("출발 시간이 지난 노선 - 완료 대상이 없으면 후속 조회를 하지 않는다")
    void completePathinfosAndFundings_whenTargetPathinfoDoesNotExist_doesNotFindAllPathinfos() {
        // Given
        given(pathinfoRepository.findPathinfosWithFunding(
                PathinfoStatus.PENDING,
                NOW,
                FundingStatus.CONFIRMED
        )).willReturn(List.of());

        // When
        fundingTimelineProcessor.completePathinfosAndFundings(NOW);

        // Then
        verify(pathinfoRepository, never())
                .findByFunding_FundingIdInAndStatusNot(
                        List.of(),
                        PathinfoStatus.CANCELLED
                );
    }

    private Funding funding(
            Long fundingId,
            FundingStatus status,
            int minParticipants
    ) {
        Funding funding = Funding.create(
                member(1L),
                "Football Match Bus",
                "Ride together",
                TODAY.plusDays(10),
                BusType.BUS_45,
                minParticipants,
                500000,
                TripType.ROUND
        );
        ReflectionTestUtils.setField(funding, "fundingId", fundingId);
        ReflectionTestUtils.setField(funding, "status", status);
        return funding;
    }

    private Pathinfo pathinfo(
            Long pathinfoId,
            Funding funding,
            Direction direction,
            PathinfoStatus status
    ) {
        Pathinfo pathinfo = Pathinfo.create(
                funding,
                NOW.minusHours(1),
                "Incheon Terminal",
                Region.INCHEON,
                "Seoul Stadium",
                Region.SEOUL_A,
                direction
        );
        ReflectionTestUtils.setField(pathinfo, "pathinfoId", pathinfoId);
        ReflectionTestUtils.setField(pathinfo, "status", status);
        return pathinfo;
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
                .createdAt(NOW.minusDays(10))
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
