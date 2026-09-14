package com.samosajunction.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssistantChatRequest(
        @NotBlank @Size(max = 2000) String message,
        @Valid @Size(max = 24) List<AssistantHistoryMessage> history
) {
    public List<AssistantHistoryMessage> safeHistory() {
        return history == null ? List.of() : history;
    }
}
