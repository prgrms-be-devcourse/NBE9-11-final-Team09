package com.back.team9.moyeota.domain.member.service.auth;

import com.back.team9.moyeota.domain.member.dto.auth.KakaoUserInfoResponse;
import com.back.team9.moyeota.domain.member.dto.auth.MemberLoginResult;
import com.back.team9.moyeota.domain.member.dto.auth.MemberSocialLoginRequest;
import com.back.team9.moyeota.domain.member.entity.Member;
import com.back.team9.moyeota.domain.member.entity.MemberStatus;
import com.back.team9.moyeota.domain.member.entity.Provider;
import com.back.team9.moyeota.domain.member.infrastructure.social.KakaoSocialLoginClient;
import com.back.team9.moyeota.domain.member.repository.MemberRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import com.back.team9.moyeota.global.jwt.JwtTokenProvider;
import com.back.team9.moyeota.global.jwt.JwtTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("회원 소셜 로그인 서비스 테스트")
class MemberSocialLoginServiceTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-06-23T00:00:00Z");
    private static final ZoneId FIXED_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_DATE_TIME =
            LocalDateTime.ofInstant(FIXED_INSTANT, FIXED_ZONE);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KakaoSocialLoginClient kakaoSocialLoginClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Clock clock;

    @InjectMocks
    private MemberSocialLoginService memberSocialLoginService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(FIXED_ZONE);
    }

    @Test
    @DisplayName("기존 카카오 회원이면 회원 생성 없이 JWT를 발급한다")
    void loginWithExistingKakaoMemberReturnsTokens() {
        MemberSocialLoginRequest request = createRequest();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member member = createKakaoMember(MemberStatus.ACTIVE);
        JwtTokenResponse tokens = createTokens();

        when(kakaoSocialLoginClient.getUserInfo(request.accessToken()))
                .thenReturn(userInfo);
        when(memberRepository.findByProviderAndProviderId(
                Provider.KAKAO,
                "123456789"
        )).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createTokens(member.getMemberId()))
                .thenReturn(tokens);

        MemberLoginResult result = memberSocialLoginService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.response().user().userId()).isEqualTo(1L);
        assertThat(result.response().user().email())
                .isEqualTo("kakao@example.com");

        verify(memberRepository, never()).save(any(Member.class));
        verify(jwtTokenProvider).createTokens(member.getMemberId());
    }

    @Test
    @DisplayName("신규 카카오 회원이면 회원을 생성하고 JWT를 발급한다")
    void loginWithNewKakaoMemberCreatesMemberAndReturnsTokens() {
        MemberSocialLoginRequest request = createRequest();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member savedMember = createKakaoMember(MemberStatus.ACTIVE);
        JwtTokenResponse tokens = createTokens();

        when(kakaoSocialLoginClient.getUserInfo(request.accessToken()))
                .thenReturn(userInfo);
        when(memberRepository.findByProviderAndProviderId(
                Provider.KAKAO,
                "123456789"
        )).thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("kakao@example.com"))
                .thenReturn(false);
        when(memberRepository.existsByNickname("카카오유저_123456789"))
                .thenReturn(false);
        when(memberRepository.save(any(Member.class)))
                .thenReturn(savedMember);
        when(jwtTokenProvider.createTokens(savedMember.getMemberId()))
                .thenReturn(tokens);

        MemberLoginResult result = memberSocialLoginService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().user().email())
                .isEqualTo("kakao@example.com");

        ArgumentCaptor<Member> memberCaptor =
                ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());

        Member newMember = memberCaptor.getValue();
        assertThat(newMember.getEmail()).isEqualTo("kakao@example.com");
        assertThat(newMember.getPassword()).isNull();
        assertThat(newMember.getName()).isEqualTo("카카오유저_123456789");
        assertThat(newMember.getNickname()).isEqualTo("카카오유저_123456789");
        assertThat(newMember.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(newMember.getProviderId()).isEqualTo("123456789");
        assertThat(newMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(newMember.getCreatedAt()).isEqualTo(FIXED_DATE_TIME);
    }

    @Test
    @DisplayName("소셜 이메일이 기존 회원 이메일과 중복되면 예외가 발생한다")
    void loginWithDuplicateEmailThrowsException() {
        MemberSocialLoginRequest request = createRequest();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();

        when(kakaoSocialLoginClient.getUserInfo(request.accessToken()))
                .thenReturn(userInfo);
        when(memberRepository.findByProviderAndProviderId(
                Provider.KAKAO,
                "123456789"
        )).thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("kakao@example.com"))
                .thenReturn(true);

        assertBusinessException(
                () -> memberSocialLoginService.login(request),
                ErrorCode.DUPLICATE_EMAIL
        );

        verify(memberRepository, never()).save(any(Member.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("소셜 이메일이 제공되지 않으면 예외가 발생한다")
    void loginWithoutEmailThrowsException() {
        MemberSocialLoginRequest request = createRequest();
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                123456789L,
                new KakaoUserInfoResponse.KakaoAccount(
                        null,
                        new KakaoUserInfoResponse.Profile("카카오유저")
                )
        );

        when(kakaoSocialLoginClient.getUserInfo(request.accessToken()))
                .thenReturn(userInfo);
        when(memberRepository.findByProviderAndProviderId(
                Provider.KAKAO,
                "123456789"
        )).thenReturn(Optional.empty());

        assertBusinessException(
                () -> memberSocialLoginService.login(request),
                ErrorCode.SOCIAL_EMAIL_NOT_PROVIDED
        );

        verify(memberRepository, never()).save(any(Member.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("정지 회원이면 소셜 로그인에 실패한다")
    void loginWithSuspendedMemberThrowsException() {
        MemberSocialLoginRequest request = createRequest();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member member = createKakaoMember(MemberStatus.SUSPENDED);

        when(kakaoSocialLoginClient.getUserInfo(request.accessToken()))
                .thenReturn(userInfo);
        when(memberRepository.findByProviderAndProviderId(
                Provider.KAKAO,
                "123456789"
        )).thenReturn(Optional.of(member));

        assertBusinessException(
                () -> memberSocialLoginService.login(request),
                ErrorCode.USER_SUSPENDED
        );

        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("탈퇴 회원이면 소셜 로그인에 실패한다")
    void loginWithWithdrawnMemberThrowsException() {
        MemberSocialLoginRequest request = createRequest();
        KakaoUserInfoResponse userInfo = createKakaoUserInfo();
        Member member = createKakaoMember(MemberStatus.WITHDRAWN);

        when(kakaoSocialLoginClient.getUserInfo(request.accessToken()))
                .thenReturn(userInfo);
        when(memberRepository.findByProviderAndProviderId(
                Provider.KAKAO,
                "123456789"
        )).thenReturn(Optional.of(member));

        assertBusinessException(
                () -> memberSocialLoginService.login(request),
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

    private MemberSocialLoginRequest createRequest() {
        return new MemberSocialLoginRequest(
                Provider.KAKAO,
                "kakao-access-token"
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
                .createdAt(FIXED_DATE_TIME)
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