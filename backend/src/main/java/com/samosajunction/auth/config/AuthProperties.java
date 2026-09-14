package com.samosajunction.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "samosa.auth")
public record AuthProperties(
        Duration resetTokenTtl,
        boolean exposeResetPath,
        String publicAppUrl
) {
}
