package com.samosajunction.auth.config;

import com.samosajunction.auth.security.JwtProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// AI-ASSISTED: Cursor
// PROMPT: Fail fast in prod when default JWT secret is used
// ACCEPTED-BY: omprakash
@Component
@Profile("prod")
public class JwtSecretProductionValidator {

    private static final String DEV_DEFAULT_SECRET = "local-dev-only-change-me-32bytes-min-key!";

    private final JwtProperties jwtProperties;

    public JwtSecretProductionValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecret() {
        if (DEV_DEFAULT_SECRET.equals(jwtProperties.secret())) {
            throw new IllegalStateException(
                    "Production startup blocked: set JWT_SECRET to a unique value of at least 32 bytes"
            );
        }
    }
}
