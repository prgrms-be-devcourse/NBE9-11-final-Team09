package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupData;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 등록 서비스 테스트")
class MemberRegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberRegistrationService memberRegistrationService;

    @Test
    @DisplayName("가입 대기 정보로 활성 회원을 등록한다")
    void registerWithValidPendingSignupSavesActiveMember() {
        PendingSignupData signupData = createPendingSignupData();

        memberRegistrationService.register(signupData);

        ArgumentCaptor<Member> memberCaptor =
                ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());

        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getEmail()).isEqualTo(signupData.email());
        assertThat(savedMember.getPassword())
                .isEqualTo(signupData.encodedPassword());
        assertThat(savedMember.getName()).isEqualTo(signupData.name());
        assertThat(savedMember.getNickname())
                .isEqualTo(signupData.nickname());
        assertThat(savedMember.getPhoneNumber())
                .isEqualTo(signupData.phoneNumber());
        assertThat(savedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(savedMember.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("인증 대기 중 이메일이 중복되면 회원을 등록하지 않는다")
    void registerWithDuplicateEmailThrowsException() {
        PendingSignupData signupData = createPendingSignupData();
        when(memberRepository.existsByEmail(signupData.email()))
                .thenReturn(true);

        assertBusinessException(
                () -> memberRegistrationService.register(signupData),
                ErrorCode.DUPLICATE_EMAIL
        );

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("인증 대기 중 닉네임이 중복되면 회원을 등록하지 않는다")
    void registerWithDuplicateNicknameThrowsException() {
        PendingSignupData signupData = createPendingSignupData();
        when(memberRepository.existsByNickname(signupData.nickname()))
                .thenReturn(true);

        assertBusinessException(
                () -> memberRegistrationService.register(signupData),
                ErrorCode.DUPLICATE_NICKNAME
        );

        verify(memberRepository, never()).save(any(Member.class));
    }

    private void assertBusinessException(
            Runnable action,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(expectedErrorCode);
    }

    private PendingSignupData createPendingSignupData() {
        return new PendingSignupData(
                "moyeota@example.com",
                "encoded-password",
                "홍길동",
                "모여타요",
                "010-1234-5678"
        );
    }
}
