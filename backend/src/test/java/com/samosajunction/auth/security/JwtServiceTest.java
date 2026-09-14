package com.samosajunction.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void createAndParseRoundTrip() {
        var jwtService = new JwtService(new JwtProperties("local-dev-only-change-me-32bytes-min-key!", Duration.ofHours(1)));
        UUID userId = UUID.randomUUID();

        String token = jwtService.createAccessToken(userId, "ada@samosa.test");
        var claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("ada@samosa.test");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofMinutes(5))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void rejectsTamperedToken() {
        var jwtService = new JwtService(new JwtProperties("local-dev-only-change-me-32bytes-min-key!", Duration.ofHours(1)));
        String token = jwtService.createAccessToken(UUID.randomUUID(), "ada@samosa.test");

        assertThatThrownBy(() -> jwtService.parse(token + "x"))
                .isInstanceOf(RuntimeException.class);
    }
}
