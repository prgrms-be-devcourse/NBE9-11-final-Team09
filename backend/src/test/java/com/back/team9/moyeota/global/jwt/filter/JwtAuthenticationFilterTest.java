package com.back.team9.moyeota.global.jwt.filter;

import com.back.team9.moyeota.global.jwt.type.PrincipalType;

import com.back.team9.moyeota.global.jwt.resolver.JwtTokenResolver;

import com.back.team9.moyeota.global.jwt.provider.JwtTokenProvider;

import com.back.team9.moyeota.global.jwt.dto.JwtAuthenticationInfo;

import com.back.team9.moyeota.global.jwt.blacklist.JwtBlacklistService;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
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

        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.of("access-token"));
        when(jwtTokenProvider.findAuthenticationInfo("access-token"))
                .thenReturn(Optional.of(new JwtAuthenticationInfo(
                        1L,
                        PrincipalType.MEMBER,
                        "MEMBER",
                        "access-jti"
                )));
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
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MEMBER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 관리자 Access Token이면 관리자 권한을 등록한다")
    void validAdminAccessTokenSetsAdminAuthorities() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer admin-access-token");

        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.of("admin-access-token"));
        when(jwtTokenProvider.findAuthenticationInfo(
                "admin-access-token"
        )).thenReturn(Optional.of(new JwtAuthenticationInfo(
                1L,
                PrincipalType.ADMIN,
                "SUPER_ADMIN",
                "admin-access-jti"
        )));
        when(jwtBlacklistService.isBlacklisted("admin-access-jti"))
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
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

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

        when(jwtTokenResolver.findToken(request))
                .thenReturn(Optional.of("invalid-token"));
        when(jwtTokenProvider.findAuthenticationInfo("invalid-token"))
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
        when(jwtTokenProvider.findAuthenticationInfo("access-token"))
                .thenReturn(Optional.of(new JwtAuthenticationInfo(
                        1L,
                        PrincipalType.MEMBER,
                        "MEMBER",
                        "access-jti"
                )));
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

    @Test
    @DisplayName("이미 인증된 요청은 기존 인증 정보를 유지한다")
    void alreadyAuthenticatedRequestKeepsExistingAuthentication()
            throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "existing-user",
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        // When
        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuthentication);

        verifyNoInteractions(
                jwtTokenResolver,
                jwtTokenProvider,
                jwtBlacklistService
        );
        verify(filterChain).doFilter(request, response);
    }
}
