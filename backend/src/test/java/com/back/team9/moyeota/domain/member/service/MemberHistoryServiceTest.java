package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.funding.entity.Funding;
import com.back.team9.moyeota.domain.funding.entity.FundingStatus;
import com.back.team9.moyeota.domain.member.dto.MemberParticipationResponse;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberParticipationQueryRepository;
import com.back.team9.moyeota.domain.participation.entity.Participation;
import com.back.team9.moyeota.domain.participation.entity.ParticipationPaymentStatus;
import com.back.team9.moyeota.domain.participation.entity.ParticipationStatus;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 내역 서비스 테스트")
class MemberHistoryServiceTest {

    @Mock
    private MemberParticipationQueryRepository participationQueryRepository;

    @InjectMocks
    private MemberHistoryService memberHistoryService;

    @Test
    @DisplayName("내 참여 내역을 최신순 페이징으로 조회한다")
    void getMyParticipationsReturnsPagedParticipationHistory() {
        // Given
        Participation participation = createParticipation();

        when(participationQueryRepository.findByMember_MemberId(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(PageRequest.class)
        )).thenReturn(new PageImpl<>(
                List.of(participation),
                PageRequest.of(0, 10),
                1
        ));

        // When
        PageResponse<MemberParticipationResponse> response =
                memberHistoryService.getMyParticipations(1L, 0, 10);

        // Then
        assertThat(response.content()).hasSize(1);

        MemberParticipationResponse content = response.content().getFirst();
        assertThat(content.participationId()).isEqualTo(1L);
        assertThat(content.fundingId()).isEqualTo(10L);
        assertThat(content.fundingTitle()).isEqualTo("강남 → 부산 합승 모집");
        assertThat(content.departureDate())
                .isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(content.status()).isEqualTo(ParticipationStatus.ACTIVE);
        assertThat(content.paymentStatus())
                .isEqualTo(ParticipationPaymentStatus.ACTIVE);

        assertThat(response.pageInfo().currentPage()).isZero();
        assertThat(response.pageInfo().totalPages()).isEqualTo(1);
        assertThat(response.pageInfo().totalElements()).isEqualTo(1);
        assertThat(response.pageInfo().size()).isEqualTo(10);
        assertThat(response.pageInfo().isLast()).isTrue();
    }

    @Test
    @DisplayName("내 참여 내역 조회 시 생성일 기준 내림차순 정렬을 적용한다")
    void getMyParticipationsUsesCreatedAtDescendingSort() {
        // Given
        when(participationQueryRepository.findByMember_MemberId(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(PageRequest.class)
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        ));

        // When
        memberHistoryService.getMyParticipations(1L, 0, 10);

        // Then
        ArgumentCaptor<PageRequest> pageRequestCaptor =
                ArgumentCaptor.forClass(PageRequest.class);

        verify(participationQueryRepository).findByMember_MemberId(
                org.mockito.ArgumentMatchers.eq(1L),
                pageRequestCaptor.capture()
        );

        PageRequest pageRequest = pageRequestCaptor.getValue();

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(10);
        assertThat(pageRequest.getSort().getOrderFor("createdAt"))
                .isNotNull();
        assertThat(pageRequest.getSort().getOrderFor("createdAt")
                .isDescending()).isTrue();
    }

    private Participation createParticipation() {
        return Participation.builder()
                .participationId(1L)
                .funding(createFunding())
                .member(createMember())
                .paymentStatus(ParticipationPaymentStatus.ACTIVE)
                .finalAmount(0)
                .status(ParticipationStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .build();
    }

    private Funding createFunding() {
        return Funding.builder()
                .fundingId(10L)
                .member(createMember())
                .title("강남 → 부산 합승 모집")
                .content("부산행 버스 모집")
                .departureDate(LocalDate.of(2026, 7, 10))
                .status(FundingStatus.RECRUITING)
                .minParticipants(20)
                .maxParticipants(45)
                .paybackHold(false)
                .createdAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .build();
    }

    private Member createMember() {
        return Member.builder()
                .memberId(1L)
                .email("member@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("모여타요")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();
    }
}