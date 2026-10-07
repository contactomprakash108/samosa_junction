package com.samosajunction.auth.service;

import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.entity.RefreshToken;
import com.samosajunction.auth.repository.RefreshTokenRepository;
import com.samosajunction.auth.security.TokenHasher;
import com.samosajunction.auth.support.SecurityAuditLogger;
import com.samosajunction.common.exception.UnauthorizedException;
import com.samosajunction.user.entity.Role;
import com.samosajunction.user.entity.RoleName;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityAuditLogger auditLogger;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                userRepository,
                new AuthProperties(
                        Duration.ofMinutes(30),
                        false,
                        "http://localhost:5173",
                        Duration.ofDays(7),
                        10,
                        Duration.ofMinutes(15)
                ),
                auditLogger
        );
    }

    @Test
    void rotateIssuesNewTokenAndRevokesOldOne() {
        UUID userId = UUID.randomUUID();
        String rawToken = TokenHasher.generateOpaqueToken();
        var stored = new RefreshToken(userId, TokenHasher.hashToken(rawToken), Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());

        User user = new User("ada@samosa.test", "hash", "Ada");
        user.addRole(new Role(RoleName.CUSTOMER));
        ReflectionTestUtils.setField(user, "id", userId);

        when(refreshTokenRepository.findByTokenHash(TokenHasher.hashToken(rawToken))).thenReturn(Optional.of(stored));
        when(userRepository.findWithRolesById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            ReflectionTestUtils.setField(token, "id", UUID.randomUUID());
            return token;
        });

        var result = refreshTokenService.rotate(rawToken);

        assertThat(result.user().getId()).isEqualTo(userId);
        assertThat(result.refreshToken().rawToken()).isNotBlank();
        assertThat(stored.getRevokedAt()).isNotNull();
        verify(auditLogger).refreshTokenRotated(eq(userId), eq(stored.getId()), any(UUID.class));
    }

    @Test
    void rotateDetectsReuseAndRevokesAllUserTokens() {
        UUID userId = UUID.randomUUID();
        String rawToken = TokenHasher.generateOpaqueToken();
        var stored = new RefreshToken(userId, TokenHasher.hashToken(rawToken), Instant.now().plusSeconds(3600));
        stored.revoke(Instant.now(), UUID.randomUUID());
        when(refreshTokenRepository.findByTokenHash(TokenHasher.hashToken(rawToken))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(UnauthorizedException.class);
        verify(refreshTokenRepository).revokeAllActiveForUser(eq(userId), any(Instant.class));
        verify(auditLogger).refreshTokenReuseDetected(userId);
    }
}
