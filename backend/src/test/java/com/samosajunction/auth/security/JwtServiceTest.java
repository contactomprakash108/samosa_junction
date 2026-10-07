package com.samosajunction.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static JwtProperties testProperties() {
        return new JwtProperties(
                "test-jwt-secret-at-least-32-bytes!!",
                Duration.ofHours(1),
                "samosa-junction-test",
                "samosa-junction-api-test",
                Duration.ofSeconds(60)
        );
    }

    @Test
    void createAndParseRoundTrip() {
        var jwtService = new JwtService(testProperties());
        UUID userId = UUID.randomUUID();

        String token = jwtService.createAccessToken(userId);
        var claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo("samosa-junction-test");
        assertThat(claims.getAudience()).containsExactly("samosa-junction-api-test");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(
                "too-short",
                Duration.ofMinutes(5),
                "issuer",
                "audience",
                Duration.ofSeconds(30)
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void rejectsTamperedToken() {
        var jwtService = new JwtService(testProperties());
        String token = jwtService.createAccessToken(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parse(token + "x"))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rejectsWrongAudience() {
        var jwtService = new JwtService(testProperties());
        String token = jwtService.createAccessToken(UUID.randomUUID());
        var otherAudience = new JwtService(new JwtProperties(
                "test-jwt-secret-at-least-32-bytes!!",
                Duration.ofHours(1),
                "samosa-junction-test",
                "other-audience",
                Duration.ofSeconds(60)
        ));

        assertThatThrownBy(() -> otherAudience.parse(token))
                .isInstanceOf(InvalidJwtException.class);
    }
}
