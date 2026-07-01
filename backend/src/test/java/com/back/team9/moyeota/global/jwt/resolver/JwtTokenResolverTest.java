package com.back.team9.moyeota.global.jwt.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 토큰 추출기 테스트")
class JwtTokenResolverTest {

    private final JwtTokenResolver jwtTokenResolver =
            new JwtTokenResolver();

    @Test
    @DisplayName("Bearer Authorization 헤더에서 토큰을 추출한다")
    void findTokenReturnsBearerToken() {
        assertThat(jwtTokenResolver.findToken(
                "Bearer access-token"
        )).contains("access-token");
    }

    @Test
    @DisplayName("Bearer 접두사는 대소문자를 구분하지 않는다")
    void findTokenAcceptsLowercaseBearerPrefix() {
        assertThat(jwtTokenResolver.findToken(
                "bearer access-token"
        )).contains("access-token");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 빈 Optional을 반환한다")
    void findTokenWithoutAuthorizationReturnsEmpty() {
        assertThat(jwtTokenResolver.findToken((String) null))
                .isEmpty();
    }

    @Test
    @DisplayName("Bearer 뒤에 토큰이 없으면 빈 Optional을 반환한다")
    void findTokenWithEmptyBearerValueReturnsEmpty() {
        assertThat(jwtTokenResolver.findToken("Bearer "))
                .isEmpty();
    }

    @Test
    @DisplayName("HTTP 요청의 Authorization 헤더에서 토큰을 추출한다")
    void findTokenFromRequestReturnsToken() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");

        // When / Then
        assertThat(jwtTokenResolver.findToken(request))
                .contains("access-token");
    }
}