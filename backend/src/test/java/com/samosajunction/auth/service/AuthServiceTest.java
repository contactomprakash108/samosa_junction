package com.samosajunction.auth.service;


import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.dto.ForgotPasswordRequest;
import com.samosajunction.auth.dto.LoginRequest;
import com.samosajunction.auth.dto.RegisterRequest;
import com.samosajunction.auth.dto.ResetPasswordRequest;
import com.samosajunction.auth.entity.PasswordResetToken;
import com.samosajunction.auth.repository.PasswordResetTokenRepository;
import com.samosajunction.auth.security.JwtService;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.UnauthorizedException;
import com.samosajunction.notification.service.NotificationService;
import com.samosajunction.user.entity.Role;
import com.samosajunction.user.entity.RoleName;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.RoleRepository;
import com.samosajunction.user.repository.UserRepository;
import com.samosajunction.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private WalletService walletService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private NotificationService notificationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthProperties authProperties = new AuthProperties(
            Duration.ofMinutes(30),
            true,
            "http://localhost:5173"
    );

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                jwtService,
                walletService,
                passwordResetTokenRepository,
                authProperties,
                notificationService
        );
    }

    @Test
    void registerHashesPasswordAndIssuesToken() {
        when(userRepository.existsByEmailIgnoreCase("ada@samosa.test")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(new Role(RoleName.CUSTOMER)));
        when(jwtService.createAccessToken(any(), any())).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });

        var response = authService.register(
                new RegisterRequest("Ada@samosa.test", "password1", "Ada Lovelace")
        );

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        verify(walletService).createForNewUser(saved.getValue().getId());
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@samosa.test");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password1");
        assertThat(passwordEncoder.matches("password1", saved.getValue().getPasswordHash())).isTrue();
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@samosa.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("ada@samosa.test", "password1", "Ada")
        )).isInstanceOf(ResourceConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = new User("ada@samosa.test", passwordEncoder.encode("password1"), "Ada");
        user.addRole(new Role(RoleName.CUSTOMER));
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase("ada@samosa.test")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user.getId(), user.getEmail())).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = authService.login(new LoginRequest("ADA@samosa.test", "password1"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("ada@samosa.test");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@samosa.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@samosa.test", "password1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("ada@samosa.test", passwordEncoder.encode("password1"), "Ada");
        when(userRepository.findByEmailIgnoreCase("ada@samosa.test")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@samosa.test", "wrong-pass")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        verify(jwtService, never()).createAccessToken(any(), any());
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@samosa.test")).thenReturn(Optional.empty());

        var response = authService.forgotPassword(new ForgotPasswordRequest("missing@samosa.test"));

        assertThat(response.resetPath()).isNull();
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void forgotPasswordReturnsResetPathWhenExposed() {
        User user = new User("ada@samosa.test", passwordEncoder.encode("password1"), "Ada");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase("ada@samosa.test")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.forgotPassword(new ForgotPasswordRequest("ADA@samosa.test"));

        assertThat(response.resetPath()).startsWith("/reset-password?token=");
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(notificationService).notifyPasswordReset(eq("ada@samosa.test"), anyString());
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("nope", "password1")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void resetPasswordUpdatesHashAndConsumesToken() {
        User user = new User("ada@samosa.test", passwordEncoder.encode("password1"), "Ada");
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        String rawToken = "reset-token";
        var stored = new PasswordResetToken(userId, AuthService.hashToken(rawToken), Instant.now().plusSeconds(600));
        when(passwordResetTokenRepository.findByTokenHash(AuthService.hashToken(rawToken)))
                .thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var response = authService.resetPassword(new ResetPasswordRequest(rawToken, "newpass12"));

        assertThat(response.message()).contains("Password updated");
        assertThat(passwordEncoder.matches("newpass12", user.getPasswordHash())).isTrue();
        assertThat(stored.getUsedAt()).isNotNull();
    }
}
