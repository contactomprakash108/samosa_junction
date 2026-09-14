package com.samosajunction.testsupport;

import com.samosajunction.auth.security.JsonAccessDeniedHandler;
import com.samosajunction.auth.security.JsonAuthenticationEntryPoint;
import com.samosajunction.auth.security.JwtAuthenticationFilter;
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
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = "samosa.cors.allowed-origins=http://localhost:5173")
public @interface SecureWebMvc {
}
