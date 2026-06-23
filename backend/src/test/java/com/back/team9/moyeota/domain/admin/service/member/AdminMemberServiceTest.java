package com.back.team9.moyeota.domain.admin.service.member;

import com.back.team9.moyeota.domain.admin.dto.member.AdminMemberDetailResponse;
import com.back.team9.moyeota.domain.admin.dto.member.AdminMemberListResponse;
import com.back.team9.moyeota.domain.admin.dto.member.AdminMemberWithdrawRequest;
import com.back.team9.moyeota.domain.admin.dto.member.AdminMemberWithdrawResponse;
import com.back.team9.moyeota.domain.admin.repository.member.AdminMemberQueryRepository;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 회원 서비스 테스트")
class AdminMemberServiceTest {

    @Mock
    private AdminMemberQueryRepository memberRepository;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    @DisplayName("회원 목록을 페이징 조회한다")
    void getMembersReturnsPagedMembers() {
        // Given
        PageRequest pageable = PageRequest.of(0, 20);
        when(memberRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(
                        List.of(createMember(MemberStatus.ACTIVE)),
                        pageable,
                        1
                ));

        // When
        PageResponse<AdminMemberListResponse> response =
                adminMemberService.getMembers(pageable);

        // Then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().memberId()).isEqualTo(1L);
        assertThat(response.content().getFirst().email())
                .isEqualTo("member@example.com");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 상세 정보를 조회한다")
    void getMemberReturnsMemberDetail() {
        // Given
        Member member = createMember(MemberStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.countParticipationsByMemberId(1L))
                .thenReturn(3L);
        when(memberRepository.countFundingsByMemberId(1L))
                .thenReturn(2L);
        when(memberRepository.countPaymentsByMemberId(1L))
                .thenReturn(5L);

        // When
        AdminMemberDetailResponse response = adminMemberService.getMember(1L);

        // Then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.participationCount()).isEqualTo(3L);
        assertThat(response.fundingCount()).isEqualTo(2L);
        assertThat(response.paymentCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("회원을 강제 탈퇴 처리한다")
    void withdrawMemberChangesStatusToWithdrawn() {
        // Given
        Member member = createMember(MemberStatus.ACTIVE);
        AdminMemberWithdrawRequest request =
                new AdminMemberWithdrawRequest("운영 정책 위반");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        // When
        AdminMemberWithdrawResponse response =
                adminMemberService.withdrawMember(1L, request);

        // Then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("이미 탈퇴한 회원을 강제 탈퇴 처리하면 예외가 발생한다")
    void withdrawAlreadyWithdrawnMemberThrowsException() {
        // Given
        Member member = createMember(MemberStatus.WITHDRAWN);
        AdminMemberWithdrawRequest request =
                new AdminMemberWithdrawRequest("운영 정책 위반");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        // When / Then
        assertBusinessException(
                () -> adminMemberService.withdrawMember(1L, request),
                ErrorCode.ADMIN_TARGET_ALREADY_WITHDRAWN
        );
    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 예외가 발생한다")
    void getUnknownMemberThrowsException() {
        // Given
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertBusinessException(
                () -> adminMemberService.getMember(1L),
                ErrorCode.USER_NOT_FOUND
        );

        verify(memberRepository, never())
                .countParticipationsByMemberId(anyLong());
    }

    private Member createMember(MemberStatus status) {
        return Member.builder()
                .memberId(1L)
                .email("member@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("모여타요")
                .phoneNumber("010-1234-5678")
                .status(status)
                .build();
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(expectedErrorCode)
                );
    }
}
