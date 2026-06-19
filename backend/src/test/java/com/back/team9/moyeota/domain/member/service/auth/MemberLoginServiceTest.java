package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginRequest;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 로그인 서비스 테스트")
class MemberLoginServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private MemberLoginService memberLoginService;

    @Test
    @DisplayName("올바른 로그인 정보이면 JWT와 회원 정보를 반환한다")
    void loginWithValidCredentialsReturnsTokensAndMemberInfo() {
        // Given
        MemberLoginRequest request = createLoginRequest();
        Member member = createMember(MemberStatus.ACTIVE);

        JwtTokenResponse tokens = new JwtTokenResponse(
                "access-token",
                "refresh-token",
                3600,
                1209600
        );

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )).thenReturn(true);
        when(jwtTokenProvider.createTokens(member.getMemberId()))
                .thenReturn(tokens);

        // When
        MemberLoginResult result = memberLoginService.login(request);
        MemberLoginResponse response = result.response();

        // Then
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessTokenExpiresIn()).isEqualTo(3600);
        assertThat(result.refreshTokenExpiresIn()).isEqualTo(1209600);
        assertThat(response.user().userId()).isEqualTo(member.getMemberId());
        assertThat(response.user().email()).isEqualTo(member.getEmail());

        verify(jwtTokenProvider).createTokens(member.getMemberId());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    void loginWithUnknownEmailThrowsException() {
        // Given
        MemberLoginRequest request = createLoginRequest();

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        // When / Then
        assertBusinessException(
                () -> memberLoginService.login(request),
                ErrorCode.INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void loginWithInvalidPasswordThrowsException() {
        // Given
        MemberLoginRequest request = createLoginRequest();
        Member member = createMember(MemberStatus.ACTIVE);

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )).thenReturn(false);

        // When / Then
        assertBusinessException(
                () -> memberLoginService.login(request),
                ErrorCode.INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("정지된 회원이면 로그인에 실패한다")
    void loginWithSuspendedMemberThrowsException() {
        // Given
        MemberLoginRequest request = createLoginRequest();
        Member member = createMember(MemberStatus.SUSPENDED);

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        // When / Then
        assertBusinessException(
                () -> memberLoginService.login(request),
                ErrorCode.USER_SUSPENDED
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("탈퇴한 회원이면 로그인에 실패한다")
    void loginWithWithdrawnMemberThrowsException() {
        // Given
        MemberLoginRequest request = createLoginRequest();
        Member member = createMember(MemberStatus.WITHDRAWN);

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        // When / Then
        assertBusinessException(
                () -> memberLoginService.login(request),
                ErrorCode.USER_ALREADY_WITHDRAWN
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("비밀번호가 없는 소셜 회원이 일반 로그인을 요청하면 로그인에 실패한다")
    void loginWithSocialMemberReturnsInvalidCredentials() {
        // Given
        Member socialMember = Member.builder()
                .memberId(1L)
                .email("social@example.com")
                .password(null)
                .name("홍길동")
                .nickname("소셜회원")
                .phoneNumber("010-1234-5678")
                .provider(Provider.KAKAO)
                .providerId("kakao-provider-id")
                .status(MemberStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        MemberLoginRequest request = new MemberLoginRequest(
                "social@example.com",
                "Password123!"
        );

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(socialMember));

        // When / Then
        assertThatThrownBy(() -> memberLoginService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS);

        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("회원 상태가 없으면 로그인에 실패한다")
    void loginWithNullStatusThrowsException() {
        // Given
        MemberLoginRequest request = createLoginRequest();

        Member member = Member.builder()
                .memberId(1L)
                .email("moyeota@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("모여타요")
                .phoneNumber("010-1234-5678")
                .status(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(memberRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )).thenReturn(true);

        // When / Then
        assertBusinessException(
                () -> memberLoginService.login(request),
                ErrorCode.INVALID_LOGIN_CREDENTIALS
        );

        verifyNoInteractions(jwtTokenProvider);
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

    private MemberLoginRequest createLoginRequest() {
        return new MemberLoginRequest(
                "moyeota@example.com",
                "Password123!"
        );
    }

    private Member createMember(MemberStatus status) {
        return Member.builder()
                .memberId(1L)
                .email("moyeota@example.com")
                .password("encoded-password")
                .name("홍길동")
                .nickname("모여타요")
                .phoneNumber("010-1234-5678")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
