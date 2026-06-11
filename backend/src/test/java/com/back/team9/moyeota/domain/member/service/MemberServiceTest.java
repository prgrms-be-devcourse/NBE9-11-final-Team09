package com.back.team9.moyeota.domain.member.service;

import com.back.team9.moyeota.domain.member.dto.EmailVerificationConfirmRequest;
import com.back.team9.moyeota.domain.member.dto.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.PendingMemberSignup;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.domain.member.repository.PendingMemberSignupRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PendingMemberSignupRepository pendingSignupRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("유효한 회원가입 요청 시 가입 대기 정보를 저장하고 인증 메일을 발송한다")
    void requestSignupWithValidRequestSavesPendingSignupAndSendsEmail() {
        // Given
        MemberSignupRequest request = createSignupRequest();

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation ->
                        "encoded-" + invocation.getArgument(0, String.class)
                );

        // When
        memberService.requestSignup(request);

        // Then
        ArgumentCaptor<PendingMemberSignup> signupCaptor =
                ArgumentCaptor.forClass(PendingMemberSignup.class);

        ArgumentCaptor<String> codeCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(pendingSignupRepository).save(signupCaptor.capture());
        verify(emailVerificationService).sendVerificationCode(
                eq(request.email()),
                codeCaptor.capture()
        );

        PendingMemberSignup savedSignup = signupCaptor.getValue();

        assertThat(savedSignup.getEmail()).isEqualTo(request.email());
        assertThat(savedSignup.getName()).isEqualTo(request.name());
        assertThat(savedSignup.getNickname()).isEqualTo(request.nickname());
        assertThat(savedSignup.getPhoneNumber())
                .isEqualTo(request.phoneNumber());
        assertThat(savedSignup.getEncodedPassword())
                .isEqualTo("encoded-" + request.password());
        assertThat(savedSignup.getExpiresAt())
                .isAfter(LocalDateTime.now());
        assertThat(codeCaptor.getValue()).hasSize(6);
    }

    @Test
    @DisplayName("동일한 이메일로 재요청하면 기존 가입 대기 정보를 갱신한다")
    void requestSignupWithSameEmailUpdatesExistingPendingSignup() {
        // Given
        MemberSignupRequest request = createSignupRequest();

        PendingMemberSignup existingSignup = PendingMemberSignup.create(
                request.email(),
                "old-password",
                "기존이름",
                "기존닉네임",
                "010-0000-0000",
                "old-code",
                LocalDateTime.now().plusMinutes(10)
        );

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(existingSignup));

        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation ->
                        "encoded-" + invocation.getArgument(0, String.class)
                );

        // When
        memberService.requestSignup(request);

        // Then
        verify(pendingSignupRepository).save(existingSignup);
        verify(emailVerificationService).sendVerificationCode(
                eq(request.email()),
                anyString()
        );

        assertThat(existingSignup.getName()).isEqualTo(request.name());
        assertThat(existingSignup.getNickname())
                .isEqualTo(request.nickname());
        assertThat(existingSignup.getPhoneNumber())
                .isEqualTo(request.phoneNumber());
        assertThat(existingSignup.getEncodedPassword())
                .isEqualTo("encoded-" + request.password());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생한다")
    void requestSignupWithDuplicateEmailThrowsException() {
        // Given
        MemberSignupRequest request = createSignupRequest();

        when(memberRepository.existsByEmail(request.email()))
                .thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> memberService.requestSignup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        verifyNoInteractions(pendingSignupRepository);
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 예외가 발생한다")
    void requestSignupWithDuplicateNicknameThrowsException() {
        // Given
        MemberSignupRequest request = createSignupRequest();

        when(memberRepository.existsByNickname(request.nickname()))
                .thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> memberService.requestSignup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

        verifyNoInteractions(pendingSignupRepository);
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("잘못된 이메일 형식이면 예외가 발생한다")
    void requestSignupWithInvalidEmailThrowsException() {
        // Given
        MemberSignupRequest request = new MemberSignupRequest(
                "invalid-email",
                "Password123!",
                "홍길동",
                "모여타요",
                "010-1234-5678"
        );

        // When / Then
        assertThatThrownBy(() -> memberService.requestSignup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(pendingSignupRepository);
    }

    @Test
    @DisplayName("잘못된 비밀번호 형식이면 예외가 발생한다")
    void requestSignupWithInvalidPasswordThrowsException() {
        // Given
        MemberSignupRequest request = new MemberSignupRequest(
                "moyeota@example.com",
                "password",
                "홍길동",
                "모여타요",
                "010-1234-5678"
        );

        // When / Then
        assertThatThrownBy(() -> memberService.requestSignup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_PASSWORD_FORMAT);

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(pendingSignupRepository);
    }

    @Test
    @DisplayName("잘못된 전화번호 형식이면 예외가 발생한다")
    void requestSignupWithInvalidPhoneNumberThrowsException() {
        // Given
        MemberSignupRequest request = new MemberSignupRequest(
                "moyeota@example.com",
                "Password123!",
                "홍길동",
                "모여타요",
                "01012345678"
        );

        // When / Then
        assertThatThrownBy(() -> memberService.requestSignup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_PHONE_NUMBER_FORMAT);

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(pendingSignupRepository);
    }

    @Test
    @DisplayName("올바른 인증코드 확인 시 회원가입을 완료한다")
    void confirmEmailVerificationWithValidCodeSavesMember() {
        // Given
        String verificationCode = "A1B2C3";

        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        verificationCode
                );

        PendingMemberSignup pendingSignup = createPendingSignup();

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(pendingSignup));

        when(passwordEncoder.matches(
                verificationCode,
                pendingSignup.getVerificationCodeHash()
        )).thenReturn(true);

        // When
        memberService.confirmEmailVerification(request);

        // Then
        ArgumentCaptor<Member> memberCaptor =
                ArgumentCaptor.forClass(Member.class);

        verify(memberRepository).save(memberCaptor.capture());
        verify(pendingSignupRepository).delete(pendingSignup);

        Member savedMember = memberCaptor.getValue();

        assertThat(savedMember.getEmail())
                .isEqualTo(pendingSignup.getEmail());
        assertThat(savedMember.getNickname())
                .isEqualTo(pendingSignup.getNickname());
        assertThat(savedMember.getStatus())
                .isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("잘못된 인증코드 확인 시 예외가 발생한다")
    void confirmEmailVerificationWithInvalidCodeThrowsException() {
        // Given
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "WRONG1"
                );

        PendingMemberSignup pendingSignup = createPendingSignup();

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(pendingSignup));

        when(passwordEncoder.matches(
                request.verificationCode(),
                pendingSignup.getVerificationCodeHash()
        )).thenReturn(false);

        // When / Then
        assertThatThrownBy(
                () -> memberService.confirmEmailVerification(request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_CODE);

        verify(memberRepository, never()).save(any(Member.class));
        verify(pendingSignupRepository, never())
                .delete(any(PendingMemberSignup.class));
    }

    @Test
    @DisplayName("만료된 인증코드 확인 시 예외가 발생한다")
    void confirmEmailVerificationWithExpiredCodeThrowsException() {
        // Given
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "A1B2C3"
                );

        PendingMemberSignup expiredSignup = PendingMemberSignup.create(
                request.email(),
                "encoded-password",
                "홍길동",
                "모여타요",
                "010-1234-5678",
                "encoded-code",
                LocalDateTime.now().minusMinutes(1)
        );

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(expiredSignup));

        // When / Then
        assertThatThrownBy(
                () -> memberService.confirmEmailVerification(request)
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception ->
                        ((BusinessException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.VERIFICATION_CODE_EXPIRED);

        verifyNoInteractions(memberRepository);
        verify(pendingSignupRepository, never())
                .delete(any(PendingMemberSignup.class));
    }

    private MemberSignupRequest createSignupRequest() {
        return new MemberSignupRequest(
                "moyeota@example.com",
                "Password123!",
                "홍길동",
                "모여타요",
                "010-1234-5678"
        );
    }

    private PendingMemberSignup createPendingSignup() {
        return PendingMemberSignup.create(
                "moyeota@example.com",
                "encoded-password",
                "홍길동",
                "모여타요",
                "010-1234-5678",
                "encoded-code",
                LocalDateTime.now().plusMinutes(30)
        );
    }
}