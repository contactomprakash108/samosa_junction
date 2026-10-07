package com.samosajunction.auth.controller;

import com.samosajunction.auth.dto.AuthResponse;
import com.samosajunction.auth.dto.ForgotPasswordRequest;
import com.samosajunction.auth.dto.ForgotPasswordResponse;
import com.samosajunction.auth.dto.LoginRequest;
import com.samosajunction.auth.dto.LogoutRequest;
import com.samosajunction.auth.dto.MessageResponse;
import com.samosajunction.auth.dto.RefreshTokenRequest;
import com.samosajunction.auth.dto.RegisterRequest;
import com.samosajunction.auth.dto.ResetPasswordRequest;
import com.samosajunction.auth.service.AuthService;
import com.samosajunction.auth.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// AI-ASSISTED: Cursor
// PROMPT: Add refresh and logout endpoints with client IP for login rate limiting
// ACCEPTED-BY: omprakash
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, ClientIpResolver.resolve(httpRequest));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody LogoutRequest request) {
        return authService.logout(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
