package com.samosajunction.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "samosa.kafka")
public record KafkaProperties(boolean enabled, String bootstrapServers, String topic) {
}
