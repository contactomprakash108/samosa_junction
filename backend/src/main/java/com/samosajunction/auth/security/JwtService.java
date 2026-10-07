package com.samosajunction.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: Harden JWT with iss/aud validation, clock skew, and subject-only claims
// ACCEPTED-BY: omprakash
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;
    private final String audience;
    private final long clockSkewSeconds;

    public JwtService(JwtProperties properties) {
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "samosa.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256"
            );
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = properties.expiration().toMillis();
        this.issuer = properties.issuer();
        this.audience = properties.audience();
        this.clockSkewSeconds = properties.clockSkew().getSeconds();
    }

    /**
     * Claims:
     * <ul>
     *   <li>{@code sub} — stable user id (UUID)</li>
     *   <li>{@code iss} — token issuer, validated on parse</li>
     *   <li>{@code aud} — intended API audience, validated on parse</li>
     *   <li>{@code iat}/{@code exp} — lifetime bounds with configured clock skew</li>
     * </ul>
     * Roles and email are intentionally excluded so authorization stays server-side.
     */
    public String createAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMs);
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .clockSkewSeconds(clockSkewSeconds)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidJwtException("JWT expired", ex);
        } catch (SignatureException ex) {
            throw new InvalidJwtException("JWT signature invalid", ex);
        } catch (JwtException ex) {
            throw new InvalidJwtException("JWT malformed or invalid", ex);
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
