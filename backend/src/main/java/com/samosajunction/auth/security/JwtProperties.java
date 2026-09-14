package com.samosajunction.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "samosa.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
