package com.samosajunction.auth.support;

import jakarta.servlet.http.HttpServletRequest;

// AI-ASSISTED: Cursor
// PROMPT: Resolve client IP for login rate limiting behind proxies
// ACCEPTED-BY: omprakash
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
