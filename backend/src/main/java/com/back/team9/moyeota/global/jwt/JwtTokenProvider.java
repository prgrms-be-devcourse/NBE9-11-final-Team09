package com.back.team9.moyeota.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_PRINCIPAL_TYPE = "principalType";
    private static final String CLAIM_ROLE = "role";
    private static final String MEMBER_ROLE = "MEMBER";

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}")
            long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}")
            long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public JwtTokenResponse createTokens(Long memberId) {
        String accessToken = createToken(
                memberId, PrincipalType.MEMBER, MEMBER_ROLE,
                TokenType.ACCESS, accessTokenExpiration
        );

        String refreshToken = createToken(
                memberId, PrincipalType.MEMBER, MEMBER_ROLE,
                TokenType.REFRESH, refreshTokenExpiration
        );

        return new JwtTokenResponse(
                accessToken,
                refreshToken,
                accessTokenExpiration / 1000,
                refreshTokenExpiration / 1000
        );
    }

    public JwtAccessTokenResponse createAdminAccessToken(
            Long adminId,
            String role
    ) {
        return new JwtAccessTokenResponse(
                createToken(
                        adminId,
                        PrincipalType.ADMIN,
                        role,
                        TokenType.ACCESS,
                        accessTokenExpiration
                ),
                accessTokenExpiration / 1000
        );
    }

    private String createToken(
            Long principalId,
            PrincipalType principalType,
            String role,
            TokenType tokenType,
            long expiration
    ) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expiration);

        return Jwts.builder()
                .subject(principalId.toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_PRINCIPAL_TYPE, principalType.name())
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    String createToken(
            Long memberId,
            TokenType tokenType,
            long expiration
    ) {
        return createToken(
                memberId,
                PrincipalType.MEMBER,
                MEMBER_ROLE,
                tokenType,
                expiration
        );
    }

    public Long getMemberId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    public TokenType getTokenType(String token) {
        String tokenType = getClaims(token)
                .get("tokenType", String.class);

        return TokenType.valueOf(tokenType);
    }

    public long getRemainingExpiration(String token) {
        try {
            long remaining = getClaims(token)
                    .getExpiration()
                    .getTime() - System.currentTimeMillis();

            return Math.max(remaining, 0);
        } catch (JwtException | IllegalArgumentException exception) {
            return 0;
        }
    }

    /**
     * 유효한 Access Token에서 회원 ID를 추출
     * 만료·위조·형식 오류 토큰과 Refresh Token은 빈 Optional을 반환
     */
    public Optional<Long> findMemberIdFromAccessToken(String token) {
        try {
            Claims claims = getClaims(token);

            if (!TokenType.ACCESS.name().equals(
                    claims.get("tokenType", String.class)
            )) {
                return Optional.empty();
            }

            if (!PrincipalType.MEMBER.name().equals(
                    claims.get("principalType", String.class)
            )) {
                return Optional.empty();
            }

            return Optional.of(
                    Long.valueOf(claims.getSubject())
            );
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Optional<JwtAccessTokenInfo> findAccessTokenInfo(String token) {
        try {
            Claims claims = getClaims(token);
            String tokenType = claims.get("tokenType", String.class);

            if (!TokenType.ACCESS.name().equals(tokenType)) {
                return Optional.empty();
            }

            long remainingExpiration = Math.max(
                    claims.getExpiration().getTime() - System.currentTimeMillis(),
                    0
            );

            return Optional.of(new JwtAccessTokenInfo(
                    Long.valueOf(claims.getSubject()),
                    claims.getId(),
                    remainingExpiration
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Optional<JwtAuthenticationInfo> findAuthenticationInfo(
            String token
    ) {
        try {
            Claims claims = getClaims(token);

            if (!TokenType.ACCESS.name().equals(
                    claims.get(CLAIM_TOKEN_TYPE, String.class)
            )) {
                return Optional.empty();
            }

            String principalTypeValue = claims.get(
                    CLAIM_PRINCIPAL_TYPE,
                    String.class
            );
            String role = claims.get(CLAIM_ROLE, String.class);
            String jti = claims.getId();

            if (principalTypeValue == null
                    || principalTypeValue.isBlank()
                    || role == null
                    || role.isBlank()
                    || jti == null
                    || jti.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new JwtAuthenticationInfo(
                    Long.valueOf(claims.getSubject()),
                    PrincipalType.valueOf(principalTypeValue),
                    role,
                    jti
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}