package com.samosajunction.auth.dto;

import com.samosajunction.user.dto.UserResponse;

// AI-ASSISTED: Cursor
// PROMPT: Extend auth response with refresh token fields
// ACCEPTED-BY: omprakash
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds,
        UserResponse user
) {
}
