package com.samosajunction.auth.dto;

import jakarta.validation.constraints.NotBlank;

// AI-ASSISTED: Cursor
// PROMPT: Refresh token rotation request DTO
// ACCEPTED-BY: omprakash
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
