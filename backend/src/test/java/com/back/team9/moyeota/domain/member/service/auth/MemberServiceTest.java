package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.EmailVerificationConfirmRequest;
import com.back.team9.moyeota.domain.member.dto.auth.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupData;
import com.back.team9.moyeota.domain.member.infrastructure.redis.PendingSignupRedisRepository;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원가입 서비스 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PendingSignupRedisRepository pendingSignupRepository;

    @Mock
    private MemberRegistrationService memberRegistrationService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("유효한 회원가입 요청은 Redis에 저장하고 인증 메일을 발송한다")
    void requestSignupWithValidRequestSavesPendingSignupAndSendsEmail() {
        MemberSignupRequest request = createSignupRequest();
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation ->
                        "encoded-" + invocation.getArgument(0, String.class)
                );

        memberService.requestSignup(request);

        ArgumentCaptor<PendingSignupData> signupCaptor =
                ArgumentCaptor.forClass(PendingSignupData.class);
        InOrder inOrder = inOrder(
                pendingSignupRepository,
                emailVerificationService
        );
        inOrder.verify(pendingSignupRepository)
                .save(signupCaptor.capture());
        inOrder.verify(emailVerificationService)
                .sendVerificationCode(eq(request.email()), anyString());

        PendingSignupData savedData = signupCaptor.getValue();
        assertThat(savedData.email()).isEqualTo(request.email());
        assertThat(savedData.encodedPassword())
                .isEqualTo("encoded-" + request.password());
        assertThat(savedData.name()).isEqualTo(request.name());
        assertThat(savedData.nickname()).isEqualTo(request.nickname());
        assertThat(savedData.phoneNumber())
                .isEqualTo(request.phoneNumber());
        assertThat(savedData.verificationCodeHash())
                .startsWith("encoded-");
    }

    @Test
    @DisplayName("이메일 앞뒤 공백을 제거하고 소문자로 정규화하여 처리한다")
    void requestSignupNormalizesEmailBeforeValidation() {
        // Given
        MemberSignupRequest request = new MemberSignupRequest(
                " MOYEOTA@EXAMPLE.COM ",
                "Password123!",
                "홍길동",
                "모여타",
                "010-1234-5678"
        );

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-value");

        // When
        memberService.requestSignup(request);

        // Then
        ArgumentCaptor<PendingSignupData> signupCaptor =
                ArgumentCaptor.forClass(PendingSignupData.class);

        verify(memberRepository)
                .existsByEmail("moyeota@example.com");
        verify(pendingSignupRepository)
                .save(signupCaptor.capture());
        verify(emailVerificationService)
                .sendVerificationCode(
                        eq("moyeota@example.com"),
                        anyString()
                );

        assertThat(signupCaptor.getValue().email())
                .isEqualTo("moyeota@example.com");
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입 요청을 거부한다")
    void requestSignupWithDuplicateEmailThrowsException() {
        MemberSignupRequest request = createSignupRequest();
        when(memberRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.DUPLICATE_EMAIL
        );

        verifyNoInteractions(pendingSignupRepository);
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 회원가입 요청을 거부한다")
    void requestSignupWithDuplicateNicknameThrowsException() {
        MemberSignupRequest request = createSignupRequest();
        when(memberRepository.existsByNickname(request.nickname()))
                .thenReturn(true);

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.DUPLICATE_NICKNAME
        );

        verifyNoInteractions(pendingSignupRepository);
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 회원가입 요청을 거부한다")
    void requestSignupWithInvalidEmailThrowsException() {
        MemberSignupRequest request = new MemberSignupRequest(
                "invalid-email", "Password123!", "홍길동", "모여타요",
                "010-1234-5678"
        );

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.INVALID_EMAIL_FORMAT
        );
        verifyNoInteractions(memberRepository, pendingSignupRepository);
    }

    @Test
    @DisplayName("비밀번호 형식이 잘못되면 회원가입 요청을 거부한다")
    void requestSignupWithInvalidPasswordThrowsException() {
        MemberSignupRequest request = new MemberSignupRequest(
                "moyeota@example.com", "password", "홍길동", "모여타요",
                "010-1234-5678"
        );

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.INVALID_PASSWORD_FORMAT
        );
        verifyNoInteractions(memberRepository, pendingSignupRepository);
    }

    @Test
    @DisplayName("전화번호 형식이 잘못되면 회원가입 요청을 거부한다")
    void requestSignupWithInvalidPhoneNumberThrowsException() {
        MemberSignupRequest request = new MemberSignupRequest(
                "moyeota@example.com", "Password123!", "홍길동", "모여타요",
                "01012345678"
        );

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.INVALID_PHONE_NUMBER_FORMAT
        );
        verifyNoInteractions(memberRepository, pendingSignupRepository);
    }

    @Test
    @DisplayName("인증 메일 발송에 실패하면 Redis 가입 대기 정보를 삭제한다")
    void requestSignupWhenEmailSendingFailsDeletesPendingSignup() {
        MemberSignupRequest request = createSignupRequest();
        BusinessException mailException =
                new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-value");
        doThrow(mailException).when(emailVerificationService)
                .sendVerificationCode(eq(request.email()), anyString());

        assertThatThrownBy(() -> memberService.requestSignup(request))
                .isSameAs(mailException);

        verify(pendingSignupRepository).deleteByEmail(request.email());
    }

    @Test
    @DisplayName("올바른 인증코드이면 회원 등록 후 Redis 데이터를 삭제한다")
    void confirmEmailVerificationWithValidCodeRegistersMember() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com", "A1B2C3"
                );
        PendingSignupData signupData = createPendingSignupData();
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(passwordEncoder.matches(
                request.verificationCode(),
                signupData.verificationCodeHash()
        )).thenReturn(true);

        memberService.confirmEmailVerification(request);

        InOrder inOrder = inOrder(
                memberRegistrationService,
                pendingSignupRepository
        );
        inOrder.verify(memberRegistrationService).register(signupData);
        inOrder.verify(pendingSignupRepository)
                .deleteByEmail(request.email());
    }

    @Test
    @DisplayName("인증코드가 일치하지 않으면 회원을 등록하지 않는다")
    void confirmEmailVerificationWithInvalidCodeThrowsException() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com", "WRONG1"
                );
        PendingSignupData signupData = createPendingSignupData();
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(passwordEncoder.matches(
                request.verificationCode(),
                signupData.verificationCodeHash()
        )).thenReturn(false);

        assertBusinessException(
                () -> memberService.confirmEmailVerification(request),
                ErrorCode.INVALID_VERIFICATION_CODE
        );

        verify(memberRegistrationService, never())
                .register(any(PendingSignupData.class));
        verify(pendingSignupRepository, never())
                .deleteByEmail(anyString());
    }

    @Test
    @DisplayName("Redis 가입 대기 정보가 없으면 인증코드가 만료된 것으로 처리한다")
    void confirmEmailVerificationWithoutPendingSignupThrowsException() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com", "A1B2C3"
                );
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertBusinessException(
                () -> memberService.confirmEmailVerification(request),
                ErrorCode.VERIFICATION_CODE_EXPIRED
        );

        verifyNoInteractions(memberRegistrationService);
        verify(pendingSignupRepository, never())
                .deleteByEmail(anyString());
    }

    @Test
    @DisplayName("회원 등록에 실패하면 Redis 가입 대기 정보를 유지한다")
    void confirmEmailVerificationWhenRegistrationFailsKeepsPendingSignup() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "A1B2C3"
                );
        PendingSignupData signupData = createPendingSignupData();
        BusinessException registrationException =
                new BusinessException(ErrorCode.DUPLICATE_EMAIL);

        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(passwordEncoder.matches(
                request.verificationCode(),
                signupData.verificationCodeHash()
        )).thenReturn(true);
        doThrow(registrationException)
                .when(memberRegistrationService)
                .register(signupData);

        assertThatThrownBy(
                () -> memberService.confirmEmailVerification(request)
        ).isSameAs(registrationException);

        verify(pendingSignupRepository, never())
                .deleteByEmail(anyString());
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

    private MemberSignupRequest createSignupRequest() {
        return new MemberSignupRequest(
                "moyeota@example.com", "Password123!", "홍길동", "모여타요",
                "010-1234-5678"
        );
    }

    private PendingSignupData createPendingSignupData() {
        return new PendingSignupData(
                "moyeota@example.com",
                "encoded-password",
                "홍길동",
                "모여타요",
                "010-1234-5678",
                "encoded-code"
        );
    }
}
