package com.samosajunction.auth.service;

import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.entity.RefreshToken;
import com.samosajunction.auth.repository.RefreshTokenRepository;
import com.samosajunction.auth.security.TokenHasher;
import com.samosajunction.auth.support.SecurityAuditLogger;
import com.samosajunction.common.exception.UnauthorizedException;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: Opaque refresh token minting, rotation, reuse detection, and revocation
// ACCEPTED-BY: omprakash
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;
    private final SecurityAuditLogger auditLogger;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            AuthProperties authProperties,
            SecurityAuditLogger auditLogger
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.authProperties = authProperties;
        this.auditLogger = auditLogger;
    }

    public IssuedRefreshToken issueForUser(UUID userId) {
        String rawToken = TokenHasher.generateOpaqueToken();
        Instant now = Instant.now();
        var stored = new RefreshToken(userId, TokenHasher.hashToken(rawToken), now.plus(authProperties.refreshTokenTtl()));
        refreshTokenRepository.save(stored);
        return new IssuedRefreshToken(rawToken, authProperties.refreshTokenTtl().getSeconds());
    }

    @Transactional
    public RotationResult rotate(String rawRefreshToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.hashToken(rawRefreshToken.trim()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));

        if (stored.isRevoked()) {
            handleReuse(stored);
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        if (!stored.isActive(now)) {
            stored.revoke(now);
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        User user = userRepository.findWithRolesById(stored.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));

        String nextRawToken = TokenHasher.generateOpaqueToken();
        RefreshToken replacement = new RefreshToken(
                user.getId(),
                TokenHasher.hashToken(nextRawToken),
                now.plus(authProperties.refreshTokenTtl())
        );
        refreshTokenRepository.save(replacement);
        stored.revoke(now, replacement.getId());
        auditLogger.refreshTokenRotated(user.getId(), stored.getId(), replacement.getId());
        return new RotationResult(
                user,
                new IssuedRefreshToken(nextRawToken, authProperties.refreshTokenTtl().getSeconds())
        );
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        Instant now = Instant.now();
        refreshTokenRepository.findByTokenHash(TokenHasher.hashToken(rawRefreshToken.trim()))
                .filter(token -> token.isActive(now))
                .ifPresent(token -> {
                    token.revoke(now);
                    auditLogger.refreshTokenRevoked(token.getUserId(), "logout");
                });
    }

    @Transactional
    public void revokeAllForUser(UUID userId, String reason) {
        int revoked = refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
        if (revoked > 0) {
            auditLogger.refreshTokenRevoked(userId, reason);
        }
    }

    private void handleReuse(RefreshToken stored) {
        auditLogger.refreshTokenReuseDetected(stored.getUserId());
        refreshTokenRepository.revokeAllActiveForUser(stored.getUserId(), Instant.now());
    }

    public record IssuedRefreshToken(String rawToken, long expiresInSeconds) {
    }

    public record RotationResult(User user, IssuedRefreshToken refreshToken) {
    }
}
