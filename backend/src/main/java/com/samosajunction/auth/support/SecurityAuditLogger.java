package com.samosajunction.auth.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: Structured security audit logging without secrets
// ACCEPTED-BY: omprakash
@Component
public class SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);

    public void loginSucceeded(UUID userId, String email, String clientIp) {
        log.info("security_event=login_success userId={} email={} clientIp={}", userId, email, clientIp);
    }

    public void loginFailed(String email, String clientIp, String reason) {
        log.warn("security_event=login_failure email={} clientIp={} reason={}", email, clientIp, reason);
    }

    public void passwordResetRequested(String email) {
        log.info("security_event=password_reset_requested email={}", email);
    }

    public void passwordResetCompleted(UUID userId) {
        log.info("security_event=password_reset_completed userId={}", userId);
    }

    public void refreshTokenRotated(UUID userId, UUID oldTokenId, UUID newTokenId) {
        log.info("security_event=refresh_token_rotated userId={} oldTokenId={} newTokenId={}",
                userId, oldTokenId, newTokenId);
    }

    public void refreshTokenReuseDetected(UUID userId) {
        log.warn("security_event=refresh_token_reuse_detected userId={}", userId);
    }

    public void refreshTokenRevoked(UUID userId, String reason) {
        log.info("security_event=refresh_token_revoked userId={} reason={}", userId, reason);
    }

    public void loginRateLimited(String email, String clientIp) {
        log.warn("security_event=login_rate_limited email={} clientIp={}", email, clientIp);
    }
}
