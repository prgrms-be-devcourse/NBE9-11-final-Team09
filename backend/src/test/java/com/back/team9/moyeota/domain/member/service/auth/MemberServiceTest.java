package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.EmailVerificationConfirmRequest;
import com.back.team9.moyeota.domain.member.dto.auth.EmailVerificationRequest;
import com.back.team9.moyeota.domain.member.dto.auth.MemberSignupRequest;
import com.back.team9.moyeota.domain.member.infrastructure.redis.EmailVerificationData;
import com.back.team9.moyeota.domain.member.infrastructure.redis.EmailVerificationRedisRepository;
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
    private EmailVerificationRedisRepository verificationRepository;

    @Mock
    private MemberRegistrationService memberRegistrationService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 요청은 가입 대기 정보만 저장한다")
    void requestSignupWithValidRequestSavesPendingSignupOnly() {
        MemberSignupRequest request = createSignupRequest();
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");

        memberService.requestSignup(request);

        ArgumentCaptor<PendingSignupData> captor =
                ArgumentCaptor.forClass(PendingSignupData.class);
        verify(pendingSignupRepository).save(captor.capture());

        PendingSignupData savedData = captor.getValue();
        assertThat(savedData.email()).isEqualTo(request.email());
        assertThat(savedData.encodedPassword())
                .isEqualTo("encoded-password");
        assertThat(savedData.name()).isEqualTo(request.name());
        assertThat(savedData.nickname()).isEqualTo(request.nickname());
        assertThat(savedData.phoneNumber())
                .isEqualTo(request.phoneNumber());
        verifyNoInteractions(verificationRepository, emailVerificationService);
    }

    @Test
    @DisplayName("회원가입 요청은 이메일을 정규화한다")
    void requestSignupNormalizesEmail() {
        MemberSignupRequest request = new MemberSignupRequest(
                " MOYEOTA@EXAMPLE.COM ",
                "Password123!",
                "홍길동",
                "모여타",
                "010-1234-5678"
        );
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");

        memberService.requestSignup(request);

        ArgumentCaptor<PendingSignupData> captor =
                ArgumentCaptor.forClass(PendingSignupData.class);
        verify(memberRepository).existsByEmail("moyeota@example.com");
        verify(pendingSignupRepository).save(captor.capture());
        assertThat(captor.getValue().email())
                .isEqualTo("moyeota@example.com");
    }

    @Test
    @DisplayName("중복 이메일이면 회원가입 요청을 거절한다")
    void requestSignupWithDuplicateEmailThrowsException() {
        MemberSignupRequest request = createSignupRequest();
        when(memberRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.DUPLICATE_EMAIL
        );

        verifyNoInteractions(pendingSignupRepository);
    }

    @Test
    @DisplayName("잘못된 입력 형식이면 회원가입 요청을 거절한다")
    void requestSignupWithInvalidInputThrowsException() {
        MemberSignupRequest request = new MemberSignupRequest(
                "invalid-email",
                "password",
                "홍길동",
                "모여타",
                "01012345678"
        );

        assertBusinessException(
                () -> memberService.requestSignup(request),
                ErrorCode.INVALID_EMAIL_FORMAT
        );

        verifyNoInteractions(memberRepository, pendingSignupRepository);
    }

    @Test
    @DisplayName("이메일 인증 요청은 인증정보를 저장하고 메일을 발송한다")
    void requestEmailVerificationSavesCodeAndSendsEmail() {
        EmailVerificationRequest request =
                new EmailVerificationRequest("moyeota@example.com");
        PendingSignupData signupData = createPendingSignupData();
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-code");

        memberService.requestEmailVerification(request);

        ArgumentCaptor<EmailVerificationData> dataCaptor =
                ArgumentCaptor.forClass(EmailVerificationData.class);
        ArgumentCaptor<String> codeCaptor =
                ArgumentCaptor.forClass(String.class);
        InOrder inOrder = inOrder(
                pendingSignupRepository,
                verificationRepository,
                emailVerificationService
        );
        inOrder.verify(pendingSignupRepository).save(signupData);
        inOrder.verify(verificationRepository).save(
                eq(request.email()),
                dataCaptor.capture()
        );
        inOrder.verify(emailVerificationService).sendVerificationCode(
                eq(request.email()),
                codeCaptor.capture()
        );

        assertThat(dataCaptor.getValue().verificationCodeHash())
                .isEqualTo("encoded-code");
        assertThat(codeCaptor.getValue()).hasSize(6);
    }

    @Test
    @DisplayName("가입 대기 정보가 없으면 인증 메일을 발송하지 않는다")
    void requestEmailVerificationWithoutPendingSignupThrowsException() {
        EmailVerificationRequest request =
                new EmailVerificationRequest("moyeota@example.com");
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertBusinessException(
                () -> memberService.requestEmailVerification(request),
                ErrorCode.SIGNUP_REQUEST_NOT_FOUND
        );

        verifyNoInteractions(verificationRepository, emailVerificationService);
    }

    @Test
    @DisplayName("메일 발송 실패 시 인증정보만 삭제한다")
    void requestEmailVerificationWhenSendingFailsDeletesCodeOnly() {
        EmailVerificationRequest request =
                new EmailVerificationRequest("moyeota@example.com");
        PendingSignupData signupData = createPendingSignupData();
        BusinessException mailException =
                new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-code");
        doThrow(mailException).when(emailVerificationService)
                .sendVerificationCode(eq(request.email()), anyString());

        assertThatThrownBy(() ->
                memberService.requestEmailVerification(request)
        ).isSameAs(mailException);

        verify(verificationRepository).deleteByEmail(request.email());
        verify(pendingSignupRepository, never()).deleteByEmail(anyString());
    }

    @Test
    @DisplayName("올바른 인증코드이면 회원을 등록하고 Redis 정보를 삭제한다")
    void confirmEmailVerificationWithValidCodeRegistersMember() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "A1B2C3"
                );
        PendingSignupData signupData = createPendingSignupData();
        EmailVerificationData verificationData =
                new EmailVerificationData("encoded-code");
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(verificationRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(verificationData));
        when(passwordEncoder.matches(
                request.verificationCode(),
                verificationData.verificationCodeHash()
        )).thenReturn(true);

        memberService.confirmEmailVerification(request);

        InOrder inOrder = inOrder(
                memberRegistrationService,
                verificationRepository,
                pendingSignupRepository
        );
        inOrder.verify(memberRegistrationService).register(signupData);
        inOrder.verify(verificationRepository)
                .deleteByEmail(request.email());
        inOrder.verify(pendingSignupRepository)
                .deleteByEmail(request.email());
    }

    @Test
    @DisplayName("인증코드가 일치하지 않으면 회원을 등록하지 않는다")
    void confirmEmailVerificationWithInvalidCodeThrowsException() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "WRONG1"
                );
        PendingSignupData signupData = createPendingSignupData();
        EmailVerificationData verificationData =
                new EmailVerificationData("encoded-code");
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(verificationRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(verificationData));
        when(passwordEncoder.matches(
                request.verificationCode(),
                verificationData.verificationCodeHash()
        )).thenReturn(false);

        assertBusinessException(
                () -> memberService.confirmEmailVerification(request),
                ErrorCode.INVALID_VERIFICATION_CODE
        );

        verifyNoInteractions(memberRegistrationService);
        verify(verificationRepository, never()).deleteByEmail(anyString());
        verify(pendingSignupRepository, never()).deleteByEmail(anyString());
    }

    @Test
    @DisplayName("인증정보가 없으면 만료된 인증코드로 처리한다")
    void confirmEmailVerificationWithoutCodeThrowsException() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "A1B2C3"
                );
        PendingSignupData signupData = createPendingSignupData();
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(verificationRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertBusinessException(
                () -> memberService.confirmEmailVerification(request),
                ErrorCode.VERIFICATION_CODE_EXPIRED
        );

        verifyNoInteractions(memberRegistrationService);
    }

    @Test
    @DisplayName("회원 등록 실패 시 Redis 정보를 유지한다")
    void confirmEmailVerificationWhenRegistrationFailsKeepsRedisData() {
        EmailVerificationConfirmRequest request =
                new EmailVerificationConfirmRequest(
                        "moyeota@example.com",
                        "A1B2C3"
                );
        PendingSignupData signupData = createPendingSignupData();
        EmailVerificationData verificationData =
                new EmailVerificationData("encoded-code");
        BusinessException registrationException =
                new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        when(pendingSignupRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(signupData));
        when(verificationRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(verificationData));
        when(passwordEncoder.matches(
                request.verificationCode(),
                verificationData.verificationCodeHash()
        )).thenReturn(true);
        doThrow(registrationException)
                .when(memberRegistrationService)
                .register(signupData);

        assertThatThrownBy(() ->
                memberService.confirmEmailVerification(request)
        ).isSameAs(registrationException);

        verify(verificationRepository, never()).deleteByEmail(anyString());
        verify(pendingSignupRepository, never()).deleteByEmail(anyString());
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
                "moyeota@example.com",
                "Password123!",
                "홍길동",
                "모여타",
                "010-1234-5678"
        );
    }

    private PendingSignupData createPendingSignupData() {
        return new PendingSignupData(
                "moyeota@example.com",
                "encoded-password",
                "홍길동",
                "모여타",
                "010-1234-5678"
        );
    }
}