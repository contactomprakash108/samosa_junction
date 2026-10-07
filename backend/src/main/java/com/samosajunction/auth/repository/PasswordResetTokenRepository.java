package com.samosajunction.auth.repository;

import com.samosajunction.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: Invalidate active password reset tokens when issuing a new one
// ACCEPTED-BY: omprakash
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken token
            SET token.usedAt = :usedAt
            WHERE token.userId = :userId
              AND token.usedAt IS NULL
            """)
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}
