package com.samosajunction.assistant.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssistantChatResponse(
        String reply,
        List<AssistantProductSuggestion> products
) {
    public static AssistantChatResponse text(String reply) {
        return new AssistantChatResponse(reply, List.of());
    }

    public record AssistantProductSuggestion(
            UUID id,
            String name,
            BigDecimal price,
            int stock
    ) {
    }
}
