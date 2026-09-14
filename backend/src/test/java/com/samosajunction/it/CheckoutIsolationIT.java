package com.samosajunction.it;

import com.samosajunction.auth.dto.AuthResponse;
import com.samosajunction.auth.dto.RegisterRequest;
import com.samosajunction.wallet.dto.WalletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf(value = "com.samosajunction.testsupport.DockerAvailability#isAvailable",
        disabledReason = "Docker is required for Testcontainers")
class CheckoutIsolationIT extends AbstractContainerIT {

    private static final String PANEER = "11111111-1111-4111-8111-111111111112";

    @Test
    void checkoutHidesOrdersAndComplaintsFromOtherCustomers() {
        String ada = register("ada-iso@samosa.test");
        String bob = register("bob-iso@samosa.test");

        addMoney(ada, "add-100", "100.00");
        WalletResponse replay = addMoney(ada, "add-100", "100.00");
        assertThat(replay.balance()).isEqualByComparingTo("100.00");

        addCart(ada, PANEER, 1);
        Map<?, ?> order = createOrder(ada, "ada-order-1");
        UUID orderId = UUID.fromString(order.get("id").toString());
        assertThat(order.get("status")).isEqualTo("CONFIRMED");

        ResponseEntity<Map> bobSeesOrder = rest.exchange(
                "/api/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(bearer(bob)),
                Map.class
        );
        assertThat(bobSeesOrder.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bobSeesOrder.getBody()).isNotNull();
        assertThat(bobSeesOrder.getBody().get("code")).isEqualTo("RESOURCE_NOT_FOUND");

        ResponseEntity<Map> bobComplains = rest.exchange(
                "/api/complaints",
                HttpMethod.POST,
                json(bob, "c-bob", """
                        {"orderId":"%s","category":"DAMAGED","description":"Trying to file on Ada's order."}
                        """.formatted(orderId)),
                Map.class
        );
        assertThat(bobComplains.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map> adaComplains = rest.exchange(
                "/api/complaints",
                HttpMethod.POST,
                json(ada, "c-ada", """
                        {"orderId":"%s","category":"DAMAGED","description":"My samosas arrived damaged."}
                        """.formatted(orderId)),
                Map.class
        );
        assertThat(adaComplains.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID complaintId = UUID.fromString(adaComplains.getBody().get("id").toString());

        ResponseEntity<Map> bobSeesComplaint = rest.exchange(
                "/api/complaints/" + complaintId,
                HttpMethod.GET,
                new HttpEntity<>(bearer(bob)),
                Map.class
        );
        assertThat(bobSeesComplaint.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String register(String email) {
        ResponseEntity<AuthResponse> response = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest(email, "password1", "IT User"),
                AuthResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().accessToken();
    }

    private WalletResponse addMoney(String token, String key, String amount) {
        ResponseEntity<WalletResponse> response = rest.exchange(
                "/api/wallet/add-money",
                HttpMethod.POST,
                json(token, key, "{\"amount\":%s}".formatted(amount)),
                WalletResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private void addCart(String token, String productId, int quantity) {
        ResponseEntity<Map> response = rest.exchange(
                "/api/cart/items",
                HttpMethod.POST,
                json(token, null, "{\"productId\":\"%s\",\"quantity\":%d}".formatted(productId, quantity)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Map<?, ?> createOrder(String token, String key) {
        ResponseEntity<Map> response = rest.exchange(
                "/api/orders",
                HttpMethod.POST,
                json(token, key, """
                        {"delivery":{"recipientName":"Ada","line1":"42 Baker Street","city":"Mumbai","state":"MH","pincode":"400001"}}
                        """),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private static HttpEntity<String> json(String token, String idempotencyKey, String body) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.add("Idempotency-Key", idempotencyKey);
        }
        return new HttpEntity<>(body, headers);
    }
}
