package com.samosajunction.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// AI-ASSISTED: Cursor
// PROMPT: Extend JWT properties with issuer, audience, and clock skew
// ACCEPTED-BY: omprakash
@ConfigurationProperties(prefix = "samosa.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        String issuer,
        String audience,
        Duration clockSkew
) {
}
