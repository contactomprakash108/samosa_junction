package com.samosajunction.testsupport;

// AI-ASSISTED: Cursor
// PROMPT: Extend secure WebMvc test harness with JWT and UserDetailsService beans
// ACCEPTED-BY: omprakash

import com.samosajunction.auth.security.JsonAccessDeniedHandler;
import com.samosajunction.auth.security.JsonAuthenticationEntryPoint;
import com.samosajunction.auth.security.JwtAuthenticationFilter;
import com.samosajunction.auth.security.JwtProperties;
import com.samosajunction.auth.security.JwtService;
import com.samosajunction.auth.security.SamosaUserDetailsService;
import com.samosajunction.auth.security.SecurityConfig;
import com.samosajunction.common.exception.GlobalExceptionHandler;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtProperties.class,
        JwtService.class,
        SamosaUserDetailsService.class,
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "samosa.cors.allowed-origins=http://localhost:5173",
        "samosa.jwt.secret=test-jwt-secret-at-least-32-bytes!!",
        "samosa.jwt.expiration=15m",
        "samosa.jwt.issuer=samosa-junction-test",
        "samosa.jwt.audience=samosa-junction-api-test",
        "samosa.jwt.clock-skew=60s"
})
public @interface SecureWebMvc {
}
