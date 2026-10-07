package com.samosajunction.web;

import com.samosajunction.auth.controller.AuthController;
import com.samosajunction.auth.dto.AuthResponse;
import com.samosajunction.auth.dto.ForgotPasswordResponse;
import com.samosajunction.auth.dto.MessageResponse;
import com.samosajunction.auth.security.JwtService;
import com.samosajunction.auth.service.AuthService;
import com.samosajunction.auth.service.LoginRateLimiter;
import com.samosajunction.auth.service.RefreshTokenService;
import com.samosajunction.auth.support.SecurityAuditLogger;
import com.samosajunction.common.exception.UnauthorizedException;
import com.samosajunction.testsupport.SecureWebMvc;
import com.samosajunction.user.dto.UserResponse;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@SecureWebMvc
class AuthWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private SecurityAuditLogger securityAuditLogger;

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@samosa.test","password":"short","fullName":"Ada"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void loginMapsBadCredentialsTo401() throws Exception {
        when(authService.login(any(), any())).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@samosa.test","password":"password1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void registerReturns201() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse(
                "jwt",
                "Bearer",
                900,
                "refresh",
                604800,
                new UserResponse(
                        UUID.randomUUID(),
                        "ada@samosa.test",
                        "Ada",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        Set.of("CUSTOMER"),
                        Instant.now()
                )
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@samosa.test","password":"password1","fullName":"Ada Lovelace"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void forgotPasswordIsPublic() throws Exception {
        when(authService.forgotPassword(any()))
                .thenReturn(new ForgotPasswordResponse("If an account exists for this email, a password reset link was issued.", null));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ada@samosa.test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPasswordIsPublic() throws Exception {
        when(authService.resetPassword(any())).thenReturn(new MessageResponse("Password updated. You can log in with the new password."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"abc","password":"password1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated. You can log in with the new password."));
    }

    @Test
    void refreshIsPublic() throws Exception {
        when(authService.refresh(any())).thenReturn(new AuthResponse(
                "new-jwt",
                "Bearer",
                900,
                "new-refresh",
                604800,
                new UserResponse(
                        UUID.randomUUID(),
                        "ada@samosa.test",
                        "Ada",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        Set.of("CUSTOMER"),
                        Instant.now()
                )
        ));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"abc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt"));
    }
}
