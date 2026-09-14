package com.samosajunction.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantHistoryMessage(
        @NotBlank @Size(max = 20) String role,
        @NotBlank @Size(max = 4000) String content
) {
}
