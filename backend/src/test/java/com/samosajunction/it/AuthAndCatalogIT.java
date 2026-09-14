package com.samosajunction.it;

import com.samosajunction.auth.dto.AuthResponse;
import com.samosajunction.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf(value = "com.samosajunction.testsupport.DockerAvailability#isAvailable",
        disabledReason = "Docker is required for Testcontainers")
class AuthAndCatalogIT extends AbstractContainerIT {

    @Test
    void registerThenMeThenPublicCatalog() {
        ResponseEntity<AuthResponse> registered = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest("ada-it@samosa.test", "password1", "Ada IT"),
                AuthResponse.class
        );
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody()).isNotNull();
        assertThat(registered.getBody().accessToken()).isNotBlank();
        assertThat(registered.getBody().user().roles()).contains("CUSTOMER");

        var me = rest.exchange(
                "/api/users/me",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearer(registered.getBody().accessToken())),
                Map.class
        );
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().get("email")).isEqualTo("ada-it@samosa.test");

        var catalog = rest.getForEntity("/api/products?search=paneer", Map.class);
        assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catalog.getBody()).isNotNull();
        assertThat(catalog.getBody().get("totalElements")).isEqualTo(1);
    }
}
