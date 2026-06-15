package com.back.team9.moyeota.global.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 인증 필터 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Access Token이면 회원 인증 정보를 등록한다")
    void validAccessTokenSetsAuthentication() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer access-token");

        when(jwtTokenProvider.findMemberIdFromAccessToken("access-token"))
                .thenReturn(Optional.of(1L));
        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.of("access-token"));
        when(jwtTokenProvider.findMemberIdFromAccessToken("access-token"))
                .thenReturn(Optional.of(1L));
        when(jwtTokenProvider.getJti("access-token"))
                .thenReturn("access-jti");
        when(jwtBlacklistService.isBlacklisted("access-jti"))
                .thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // Then
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(1L);
        assertThat(authentication.isAuthenticated()).isTrue();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보를 등록하지 않는다")
    void missingAuthorizationHeaderDoesNotSetAuthentication()
            throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.empty());

        // When
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verifyNoInteractions(jwtTokenProvider, jwtBlacklistService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 정보를 등록하지 않는다")
    void invalidTokenDoesNotSetAuthentication() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtTokenProvider.findMemberIdFromAccessToken("invalid-token"))
                .thenReturn(Optional.empty());
        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.of("invalid-token"));

        // When
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("블랙리스트에 등록된 Access Token은 인증하지 않는다")
    void blacklistedAccessTokenDoesNotSetAuthentication()
            throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer access-token");

        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.of("access-token"));
        when(jwtTokenProvider.findMemberIdFromAccessToken("access-token"))
                .thenReturn(Optional.of(1L));
        when(jwtTokenProvider.getJti("access-token"))
                .thenReturn("access-jti");
        when(jwtBlacklistService.isBlacklisted("access-jti"))
                .thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        verify(filterChain).doFilter(request, response);
    }
}