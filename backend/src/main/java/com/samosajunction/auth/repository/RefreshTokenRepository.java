package com.samosajunction.auth.repository;

import com.samosajunction.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: Refresh token repository with revocation queries
// ACCEPTED-BY: omprakash
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.userId = :userId
              AND token.revokedAt IS NULL
            """)
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
