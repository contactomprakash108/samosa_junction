package com.samosajunction.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samosajunction.assistant.config.AssistantProperties;
import com.samosajunction.assistant.openrouter.OpenRouterClient;
import com.samosajunction.cart.dto.CartResponse;
import com.samosajunction.cart.service.CartService;
import com.samosajunction.order.service.OrderService;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.service.ProductService;
import com.samosajunction.recommendation.service.RecommendationService;
import com.samosajunction.user.dto.UserResponse;
import com.samosajunction.user.service.UserService;
import com.samosajunction.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private WalletService walletService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private UserService userService;

    @Mock
    private SemanticMenuSearch semanticMenuSearch;

    @Mock
    private OpenRouterClient openRouterClient;

    private AssistantService assistantService;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        AssistantProperties properties = new AssistantProperties(
                "",
                "https://openrouter.ai/api/v1",
                "openai/gpt-4o-mini",
                "openai/text-embedding-3-small",
                6,
                5
        );
        assistantService = new AssistantService(
                productService,
                cartService,
                orderService,
                walletService,
                recommendationService,
                userService,
                semanticMenuSearch,
                openRouterClient,
                properties,
                new ObjectMapper()
        );
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void orderQtyAddsMatchingProductToCart() {
        when(semanticMenuSearch.search(anyString(), anyInt())).thenReturn(List.of(paneer(50)));
        when(cartService.addItem(eq(userId), eq(productId), eq(2)))
                .thenReturn(new CartResponse(List.of(), 2, new BigDecimal("80.00")));
        when(userService.getCurrentUser(userId)).thenReturn(profile(false));

        var response = assistantService.chat(userId, "Order 2 paneer samosas.");

        assertThat(response.reply()).contains("Added 2");
        assertThat(response.reply()).contains("Paneer Samosa");
        verify(cartService).addItem(userId, productId, 2);
        verify(orderService, never()).create(eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void requestedQtyAboveStockIsRefused() {
        when(semanticMenuSearch.search(anyString(), anyInt())).thenReturn(List.of(paneer(1)));

        var response = assistantService.chat(userId, "Order 5 paneer samosas.");

        assertThat(response.reply()).contains("only 1 in stock");
        verify(cartService, never()).addItem(eq(userId), eq(productId), eq(5));
        verify(orderService, never()).create(eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void paneerLoverUsesSemanticMenuHits() {
        when(semanticMenuSearch.search(anyString(), anyInt())).thenReturn(List.of(paneer(50)));

        var response = assistantService.chat(userId, "what is the best samosa for me i am paneer lover");

        assertThat(response.reply()).containsIgnoringCase("paneer");
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().getFirst().name()).isEqualTo("Paneer Samosa");
    }

    private UserResponse profile(boolean address) {
        Instant now = Instant.parse("2026-09-14T00:00:00Z");
        return new UserResponse(
                userId,
                "ada@samosa.test",
                "Ada",
                "9999999999",
                address ? "Ada" : null,
                address ? "12 Spice Lane" : null,
                address ? "Mumbai" : null,
                address ? "MH" : null,
                address ? "400001" : null,
                address,
                Set.of("CUSTOMER"),
                now
        );
    }

    private ProductResponse paneer(int stock) {
        Instant now = Instant.parse("2026-09-14T00:00:00Z");
        return new ProductResponse(
                productId,
                "Paneer Samosa",
                "Filled pastry",
                "PROTEIN",
                new BigDecimal("40.00"),
                Set.of("paneer"),
                310,
                new BigDecimal("12.00"),
                SpiceLevel.MILD,
                Set.of("VEGETARIAN"),
                true,
                stock,
                now,
                now,
                null
        );
    }
}
