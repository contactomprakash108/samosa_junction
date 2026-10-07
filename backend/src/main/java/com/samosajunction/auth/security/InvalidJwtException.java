package com.samosajunction.auth.security;

// AI-ASSISTED: Cursor
// PROMPT: Typed JWT validation failure for filter handling
// ACCEPTED-BY: omprakash
public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
