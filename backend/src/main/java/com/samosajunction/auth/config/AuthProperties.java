package com.samosajunction.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// AI-ASSISTED: Cursor
// PROMPT: Extend auth properties for refresh tokens and login rate limiting
// ACCEPTED-BY: omprakash
@ConfigurationProperties(prefix = "samosa.auth")
public record AuthProperties(
        Duration resetTokenTtl,
        boolean exposeResetPath,
        String publicAppUrl,
        Duration refreshTokenTtl,
        int maxLoginAttempts,
        Duration loginLockoutDuration
) {
}
