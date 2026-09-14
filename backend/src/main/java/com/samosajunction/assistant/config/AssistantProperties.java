package com.samosajunction.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "samosa.assistant")
public record AssistantProperties(
        String openRouterApiKey,
        String baseUrl,
        String chatModel,
        String embeddingModel,
        int maxToolRounds,
        int semanticTopK
) {
    public boolean openRouterEnabled() {
        return openRouterApiKey != null && !openRouterApiKey.isBlank();
    }
}
