package com.samosajunction.auth.service;

import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.dto.AuthResponse;
import com.samosajunction.auth.dto.ForgotPasswordRequest;
import com.samosajunction.auth.dto.ForgotPasswordResponse;
import com.samosajunction.auth.dto.LoginRequest;
import com.samosajunction.auth.dto.MessageResponse;
import com.samosajunction.auth.dto.RegisterRequest;
import com.samosajunction.auth.dto.ResetPasswordRequest;
import com.samosajunction.auth.entity.PasswordResetToken;
import com.samosajunction.auth.repository.PasswordResetTokenRepository;
import com.samosajunction.auth.security.JwtService;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.common.exception.UnauthorizedException;
import com.samosajunction.notification.service.NotificationService;
import com.samosajunction.user.dto.UserResponse;
import com.samosajunction.user.entity.RoleName;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.RoleRepository;
import com.samosajunction.user.repository.UserRepository;
import com.samosajunction.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String FORGOT_MESSAGE =
            "If an account exists for this email, a password reset link was issued.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthProperties authProperties;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            WalletService walletService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AuthProperties authProperties,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.walletService = walletService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.authProperties = authProperties;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("An account with this email already exists");
        }

        var customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("CUSTOMER role is not configured"));

        var user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim());
        user.addRole(customerRole);
        userRepository.save(user);
        walletService.createForNewUser(user.getId());

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return toAuthResponse(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        var user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled()) {
            return new ForgotPasswordResponse(FORGOT_MESSAGE, null);
        }

        String rawToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        var token = new PasswordResetToken(
                user.getId(),
                hashToken(rawToken),
                now.plus(authProperties.resetTokenTtl())
        );
        passwordResetTokenRepository.save(token);

        String resetPath = "/reset-password?token=" + rawToken;
        String resetUrl = authProperties.publicAppUrl() + resetPath;
        notificationService.notifyPasswordReset(email, resetUrl);
        log.info("Password reset issued for {} expires {}", email, token.getExpiresAt());

        if (!authProperties.exposeResetPath()) {
            return new ForgotPasswordResponse(FORGOT_MESSAGE, null);
        }
        return new ForgotPasswordResponse(FORGOT_MESSAGE, resetPath);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        var stored = passwordResetTokenRepository.findByTokenHash(hashToken(request.token().trim()))
                .orElseThrow(() -> new InvalidRequestException("This reset link is invalid or has expired"));
        Instant now = Instant.now();
        if (!stored.isUsable(now)) {
            throw new InvalidRequestException("This reset link is invalid or has expired");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new InvalidRequestException("This reset link is invalid or has expired"));
        if (!user.isEnabled()) {
            throw new InvalidRequestException("This reset link is invalid or has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        stored.markUsed(now);
        return new MessageResponse("Password updated. You can log in with the new password.");
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.createAccessToken(user.getId(), user.getEmail());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for password reset tokens", ex);
        }
    }
}
