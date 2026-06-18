package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.profile.MemberInfoResponse;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateRequest;
import com.back.team9.moyeota.domain.member.dto.profile.MemberUpdateResponse;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 프로필 서비스 테스트")
class MemberProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberProfileService memberProfileService;

    @Test
    @DisplayName("회원 ID로 내 정보를 조회한다")
    void getMyInfoReturnsMemberInfo() {
        // Given
        Member member = createMember();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // When
        MemberInfoResponse response = memberProfileService.getMyInfo(1L);

        // Then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("member@example.com");
        assertThat(response.nickname()).isEqualTo("기존닉네임");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 정보를 조회하면 예외가 발생한다")
    void getMyInfoWithUnknownMemberThrowsException() {
        // Given
        when(memberRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When / Then
        assertBusinessException(
                () -> memberProfileService.getMyInfo(1L),
                ErrorCode.USER_NOT_FOUND
        );
    }

    @Test
    @DisplayName("닉네임과 전화번호를 수정한다")
    void updateMyInfoUpdatesNicknameAndPhoneNumber() {
        // Given
        Member member = createMember();
        MemberUpdateRequest request = new MemberUpdateRequest(
                "변경닉네임",
                "010-9999-8888"
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndMemberIdNot(
                "변경닉네임",
                1L
        )).thenReturn(false);

        // When
        MemberUpdateResponse response =
                memberProfileService.updateMyInfo(1L, request);

        // Then
        assertThat(response.nickname()).isEqualTo("변경닉네임");
        assertThat(response.phoneNumber()).isEqualTo("010-9999-8888");
        assertThat(response.updatedAt()).isNotNull();

        assertThat(member.getNickname()).isEqualTo("변경닉네임");
        assertThat(member.getPhoneNumber()).isEqualTo("010-9999-8888");
    }

    @Test
    @DisplayName("전화번호만 요청하면 닉네임은 유지한다")
    void updateMyInfoWithPhoneNumberOnlyKeepsNickname() {
        // Given
        Member member = createMember();
        MemberUpdateRequest request = new MemberUpdateRequest(
                null,
                "010-9999-8888"
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // When
        MemberUpdateResponse response =
                memberProfileService.updateMyInfo(1L, request);

        // Then
        assertThat(response.nickname()).isEqualTo("기존닉네임");
        assertThat(response.phoneNumber()).isEqualTo("010-9999-8888");

        verify(memberRepository, never())
                .existsByNicknameAndMemberIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("기존 닉네임과 동일하면 중복 검사를 수행하지 않는다")
    void updateMyInfoWithSameNicknameSkipsDuplicateCheck() {
        // Given
        Member member = createMember();
        MemberUpdateRequest request = new MemberUpdateRequest(
                "기존닉네임",
                null
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // When
        memberProfileService.updateMyInfo(1L, request);

        // Then
        verify(memberRepository, never())
                .existsByNicknameAndMemberIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 수정하면 예외가 발생한다")
    void updateMyInfoWithDuplicateNicknameThrowsException() {
        // Given
        Member member = createMember();
        MemberUpdateRequest request = new MemberUpdateRequest(
                "중복닉네임",
                null
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndMemberIdNot(
                "중복닉네임",
                1L
        )).thenReturn(true);

        // When / Then
        assertBusinessException(
                () -> memberProfileService.updateMyInfo(1L, request),
                ErrorCode.DUPLICATE_NICKNAME
        );

        assertThat(member.getNickname()).isEqualTo("기존닉네임");
    }

    @Test
    @DisplayName("수정할 필드가 없으면 예외가 발생한다")
    void updateMyInfoWithoutFieldsThrowsException() {
        // Given
        MemberUpdateRequest request = new MemberUpdateRequest(null, null);

        // When / Then
        assertBusinessException(
                () -> memberProfileService.updateMyInfo(1L, request),
                ErrorCode.INVALID_INPUT_VALUE
        );

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("빈 닉네임으로 수정하면 예외가 발생한다")
    void updateMyInfoWithBlankNicknameThrowsException() {
        // Given
        MemberUpdateRequest request = new MemberUpdateRequest(" ", null);

        // When / Then
        assertBusinessException(
                () -> memberProfileService.updateMyInfo(1L, request),
                ErrorCode.INVALID_INPUT_VALUE
        );

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("회원 ID가 없으면 내 정보 조회에 실패한다")
    void getMyInfoWithNullMemberIdThrowsException() {
        // When / Then
        assertBusinessException(
                () -> memberProfileService.getMyInfo(null),
                ErrorCode.USER_NOT_FOUND
        );

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("회원 ID가 없으면 내 정보 수정에 실패한다")
    void updateMyInfoWithNullMemberIdThrowsException() {
        // Given
        MemberUpdateRequest request = new MemberUpdateRequest(
                "변경닉네임",
                "010-9999-8888"
        );

        // When / Then
        assertBusinessException(
                () -> memberProfileService.updateMyInfo(null, request),
                ErrorCode.USER_NOT_FOUND
        );

        verifyNoInteractions(memberRepository);
    }

    private Member createMember() {
        return Member.builder()
                .memberId(1L)
                .email("member@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("기존닉네임")
                .phoneNumber("010-1234-5678")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
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