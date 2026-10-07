package com.samosajunction.auth.dto;

import jakarta.validation.constraints.NotBlank;

// AI-ASSISTED: Cursor
// PROMPT: Logout request DTO for refresh token revocation
// ACCEPTED-BY: omprakash
public record LogoutRequest(@NotBlank String refreshToken) {
}
