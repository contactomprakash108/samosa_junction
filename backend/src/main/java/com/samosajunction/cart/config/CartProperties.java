package com.samosajunction.cart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "samosa.cart")
public record CartProperties(Duration ttl, int maxQuantity, String redisKeyPrefix) {
}
