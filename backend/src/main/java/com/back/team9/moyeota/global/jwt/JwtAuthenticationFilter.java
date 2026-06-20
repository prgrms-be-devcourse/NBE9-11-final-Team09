package com.back.team9.moyeota.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenResolver jwtTokenResolver;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtTokenResolver.findToken(request)
                    .flatMap(jwtTokenProvider::findAuthenticationInfo)
                    .filter(info -> !jwtBlacklistService.isBlacklisted(
                            info.jti()
                    ))
                    .ifPresent(info -> authenticate(request, info));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(
            HttpServletRequest request,
            JwtAuthenticationInfo info
    ) {
        List<SimpleGrantedAuthority> authorities =
                createAuthorities(info);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        info.principalId(),
                        null,
                        authorities
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private List<SimpleGrantedAuthority> createAuthorities(
            JwtAuthenticationInfo info
    ) {
        if (info.principalType() == PrincipalType.MEMBER) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_MEMBER")
            );
        }

        return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_" + info.role())
        );
    }
}