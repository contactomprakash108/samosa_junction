package com.samosajunction.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportMessageRequest(
        @NotBlank @Size(max = 160) String subject,
        @NotBlank @Size(min = 10, max = 2000) String body
) {
}
