package com.samosajunction.auth.service;

import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.support.SecurityAuditLogger;
import com.samosajunction.common.exception.UnauthorizedException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

// AI-ASSISTED: Cursor
// PROMPT: Redis-backed login brute-force rate limiting
// ACCEPTED-BY: omprakash
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "auth:login:fail:";

    private final StringRedisTemplate redis;
    private final AuthProperties authProperties;
    private final SecurityAuditLogger auditLogger;

    public LoginRateLimiter(
            StringRedisTemplate redis,
            AuthProperties authProperties,
            SecurityAuditLogger auditLogger
    ) {
        this.redis = redis;
        this.authProperties = authProperties;
        this.auditLogger = auditLogger;
    }

    public void checkAllowed(String clientIp, String email) {
        int ipFailures = readFailureCount(clientIpKey(clientIp));
        int emailFailures = readFailureCount(emailKey(normalizeEmail(email)));
        if (ipFailures >= authProperties.maxLoginAttempts()
                || emailFailures >= authProperties.maxLoginAttempts()) {
            auditLogger.loginRateLimited(normalizeEmail(email), clientIp);
            throw new UnauthorizedException("Too many login attempts. Try again later.");
        }
    }

    public void recordFailure(String clientIp, String email) {
        increment(clientIpKey(clientIp));
        increment(emailKey(normalizeEmail(email)));
    }

    public void recordSuccess(String clientIp, String email) {
        redis.delete(clientIpKey(clientIp));
        redis.delete(emailKey(normalizeEmail(email)));
    }

    private void increment(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, authProperties.loginLockoutDuration());
        }
    }

    private int readFailureCount(String key) {
        String value = redis.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private static String clientIpKey(String clientIp) {
        return KEY_PREFIX + "ip:" + clientIp;
    }

    private static String emailKey(String email) {
        return KEY_PREFIX + "email:" + email;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
