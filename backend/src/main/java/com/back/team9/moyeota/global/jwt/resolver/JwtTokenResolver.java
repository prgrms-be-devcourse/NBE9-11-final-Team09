package com.back.team9.moyeota.global.jwt.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JwtTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<String> findToken(HttpServletRequest request) {
        return findToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    public Optional<String> findToken(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(
                true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length()
        )) {
            return Optional.empty();
        }

        String token = authorization.substring(
                BEARER_PREFIX.length()
        ).trim();

        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}