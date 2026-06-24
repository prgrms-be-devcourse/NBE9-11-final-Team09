package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.infrastructure.social.KakaoSocialLoginClient;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoAuthorizationCodeLoginRequest;
import com.back.team9.moyeota.domain.member.dto.auth.KakaoTokenResponse;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 소셜 로그인 서비스 테스트")
class MemberSocialLoginServiceTest {

    @Mock
    private MemberSocialLoginTransactionService transactionService;

    @Mock
    private KakaoSocialLoginClient kakaoSocialLoginClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private MemberSocialLoginService memberSocialLoginService;

    @Test
    @DisplayName("기존 카카오 회원이면 회원 생성 없이 JWT를 발급한다")
    void loginWithExistingKakaoMemberReturnsTokens() {
        KakaoAuthorizationCodeLoginRequest request = createRequest();
        KakaoTokenResponse tokenResponse = createKakaoTokenResponse();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member member = createKakaoMember(MemberStatus.ACTIVE);
        JwtTokenResponse tokens = createTokens();

        when(kakaoSocialLoginClient.getToken(
                request.code(),
                request.redirectUri()
        )).thenReturn(tokenResponse);
        when(kakaoSocialLoginClient.getUserInfo(tokenResponse.accessToken()))
                .thenReturn(userInfo);
        when(transactionService.findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        )).thenReturn(member);
        when(jwtTokenProvider.createTokens(member.getMemberId()))
                .thenReturn(tokens);

        MemberLoginResult result =
                memberSocialLoginService.loginWithKakaoAuthorizationCode(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.response().user().userId()).isEqualTo(1L);
        assertThat(result.response().user().email())
                .isEqualTo("kakao@example.com");

        verify(transactionService).findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        );
        verify(jwtTokenProvider).createTokens(member.getMemberId());
    }

    @Test
    @DisplayName("소셜 이메일이 기존 회원 이메일과 중복되면 예외가 발생한다")
    void loginWithDuplicateEmailThrowsException() {
        KakaoAuthorizationCodeLoginRequest request = createRequest();
        KakaoTokenResponse tokenResponse = createKakaoTokenResponse();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();

        when(kakaoSocialLoginClient.getToken(
                request.code(),
                request.redirectUri()
        )).thenReturn(tokenResponse);
        when(kakaoSocialLoginClient.getUserInfo(tokenResponse.accessToken()))
                .thenReturn(userInfo);
        when(transactionService.findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        )).thenThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        assertBusinessException(
                () -> memberSocialLoginService.loginWithKakaoAuthorizationCode(request),
                ErrorCode.DUPLICATE_EMAIL
        );
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("소셜 이메일이 제공되지 않으면 예외가 발생한다")
    void loginWithoutEmailThrowsException() {
        KakaoAuthorizationCodeLoginRequest request = createRequest();
        KakaoTokenResponse tokenResponse = createKakaoTokenResponse();
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                123456789L,
                new KakaoUserInfoResponse.KakaoAccount(
                        null,
                        new KakaoUserInfoResponse.Profile("카카오유저")
                )
        );

        when(kakaoSocialLoginClient.getToken(
                request.code(),
                request.redirectUri()
        )).thenReturn(tokenResponse);
        when(kakaoSocialLoginClient.getUserInfo(tokenResponse.accessToken()))
                .thenReturn(userInfo);
        when(transactionService.findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        )).thenThrow(new BusinessException(ErrorCode.SOCIAL_EMAIL_NOT_PROVIDED));

        assertBusinessException(
                () -> memberSocialLoginService.loginWithKakaoAuthorizationCode(request),
                ErrorCode.SOCIAL_EMAIL_NOT_PROVIDED
        );
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("정지 회원이면 소셜 로그인에 실패한다")
    void loginWithSuspendedMemberThrowsException() {
        KakaoAuthorizationCodeLoginRequest request = createRequest();
        KakaoTokenResponse tokenResponse = createKakaoTokenResponse();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member member = createKakaoMember(MemberStatus.SUSPENDED);

        when(kakaoSocialLoginClient.getToken(
                request.code(),
                request.redirectUri()
        )).thenReturn(tokenResponse);
        when(kakaoSocialLoginClient.getUserInfo(tokenResponse.accessToken()))
                .thenReturn(userInfo);
        when(transactionService.findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        )).thenThrow(new BusinessException(ErrorCode.USER_SUSPENDED));

        assertBusinessException(
                () -> memberSocialLoginService.loginWithKakaoAuthorizationCode(request),
                ErrorCode.USER_SUSPENDED
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("탈퇴 회원이면 소셜 로그인에 실패한다")
    void loginWithWithdrawnMemberThrowsException() {
        KakaoAuthorizationCodeLoginRequest request = createRequest();
        KakaoTokenResponse tokenResponse = createKakaoTokenResponse();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member member = createKakaoMember(MemberStatus.WITHDRAWN);

        when(kakaoSocialLoginClient.getToken(
                request.code(),
                request.redirectUri()
        )).thenReturn(tokenResponse);
        when(kakaoSocialLoginClient.getUserInfo(tokenResponse.accessToken()))
                .thenReturn(userInfo);
        when(transactionService.findOrCreateSocialMember(
                Provider.KAKAO,
                userInfo
        )).thenThrow(new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN));

        assertBusinessException(
                () -> memberSocialLoginService.loginWithKakaoAuthorizationCode(request),
                ErrorCode.USER_ALREADY_WITHDRAWN
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

    private KakaoAuthorizationCodeLoginRequest createRequest() {
        return new KakaoAuthorizationCodeLoginRequest(
                "kakao-authorization-code",
                "http://localhost:3000/login/kakao/callback"
        );
    }

    private KakaoTokenResponse createKakaoTokenResponse() {
        return new KakaoTokenResponse(
                "kakao-access-token",
                "bearer",
                21599,
                "kakao-refresh-token",
                5183999,
                "account_email profile_nickname"
        );
    }

    private KakaoUserInfoResponse createKakaoUserInfo() {
        return new KakaoUserInfoResponse(
                123456789L,
                new KakaoUserInfoResponse.KakaoAccount(
                        "kakao@example.com",
                        new KakaoUserInfoResponse.Profile("카카오유저")
                )
        );
    }

    private Member createKakaoMember(MemberStatus status) {
        return Member.builder()
                .memberId(1L)
                .email("kakao@example.com")
                .password(null)
                .name("카카오유저_123456789")
                .nickname("카카오유저_123456789")
                .phoneNumber("")
                .provider(Provider.KAKAO)
                .providerId("123456789")
                .status(status)
                .build();
    }

    private JwtTokenResponse createTokens() {
        return new JwtTokenResponse(
                "access-token",
                "refresh-token",
                3600,
                1209600
        );
    }
}