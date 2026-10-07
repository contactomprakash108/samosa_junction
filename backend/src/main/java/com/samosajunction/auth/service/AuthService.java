package com.samosajunction.auth.service;

import com.samosajunction.auth.config.AuthProperties;
import com.samosajunction.auth.dto.AuthResponse;
import com.samosajunction.auth.dto.ForgotPasswordRequest;
import com.samosajunction.auth.dto.ForgotPasswordResponse;
import com.samosajunction.auth.dto.LoginRequest;
import com.samosajunction.auth.dto.LogoutRequest;
import com.samosajunction.auth.dto.MessageResponse;
import com.samosajunction.auth.dto.RefreshTokenRequest;
import com.samosajunction.auth.dto.RegisterRequest;
import com.samosajunction.auth.dto.ResetPasswordRequest;
import com.samosajunction.auth.entity.PasswordResetToken;
import com.samosajunction.auth.repository.PasswordResetTokenRepository;
import com.samosajunction.auth.security.JwtService;
import com.samosajunction.auth.security.TokenHasher;
import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.auth.support.SecurityAuditLogger;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

// AI-ASSISTED: Cursor
// PROMPT: Refactor auth orchestration to AuthenticationManager and refresh tokens
// ACCEPTED-BY: omprakash
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String FORGOT_MESSAGE =
            "If an account exists for this email, a password reset link was issued.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;
    private final NotificationService notificationService;
    private final LoginRateLimiter loginRateLimiter;
    private final SecurityAuditLogger auditLogger;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            WalletService walletService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenService refreshTokenService,
            AuthProperties authProperties,
            NotificationService notificationService,
            LoginRateLimiter loginRateLimiter,
            SecurityAuditLogger auditLogger
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.walletService = walletService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
        this.notificationService = notificationService;
        this.loginRateLimiter = loginRateLimiter;
        this.auditLogger = auditLogger;
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
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        loginRateLimiter.checkAllowed(clientIp, email);

        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
            var principal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findWithRolesById(principal.getId())
                    .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
            loginRateLimiter.recordSuccess(clientIp, email);
            auditLogger.loginSucceeded(user.getId(), user.getEmail(), clientIp);
            return toAuthResponse(user);
        } catch (BadCredentialsException | DisabledException ex) {
            loginRateLimiter.recordFailure(clientIp, email);
            auditLogger.loginFailed(email, clientIp, "invalid_credentials");
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        var rotation = refreshTokenService.rotate(request.refreshToken());
        return toAuthResponse(rotation.user(), rotation.refreshToken());
    }

    @Transactional
    public MessageResponse logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return new MessageResponse("Logged out.");
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        var user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled()) {
            return new ForgotPasswordResponse(FORGOT_MESSAGE, null);
        }

        Instant now = Instant.now();
        passwordResetTokenRepository.invalidateActiveForUser(user.getId(), now);

        String rawToken = TokenHasher.generateOpaqueToken();
        var token = new PasswordResetToken(
                user.getId(),
                TokenHasher.hashToken(rawToken),
                now.plus(authProperties.resetTokenTtl())
        );
        passwordResetTokenRepository.save(token);

        String resetPath = "/reset-password?token=" + rawToken;
        String resetUrl = authProperties.publicAppUrl() + resetPath;
        notificationService.notifyPasswordReset(email, resetUrl);
        auditLogger.passwordResetRequested(email);

        if (!authProperties.exposeResetPath()) {
            return new ForgotPasswordResponse(FORGOT_MESSAGE, null);
        }
        return new ForgotPasswordResponse(FORGOT_MESSAGE, resetPath);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        var stored = passwordResetTokenRepository.findByTokenHash(TokenHasher.hashToken(request.token().trim()))
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
        refreshTokenService.revokeAllForUser(user.getId(), "password_reset");
        auditLogger.passwordResetCompleted(user.getId());
        return new MessageResponse("Password updated. You can log in with the new password.");
    }

    private AuthResponse toAuthResponse(User user) {
        var refreshToken = refreshTokenService.issueForUser(user.getId());
        return toAuthResponse(user, refreshToken);
    }

    private AuthResponse toAuthResponse(User user, RefreshTokenService.IssuedRefreshToken refreshToken) {
        return new AuthResponse(
                jwtService.createAccessToken(user.getId()),
                "Bearer",
                jwtService.getExpirationSeconds(),
                refreshToken.rawToken(),
                refreshToken.expiresInSeconds(),
                UserResponse.from(user)
        );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
