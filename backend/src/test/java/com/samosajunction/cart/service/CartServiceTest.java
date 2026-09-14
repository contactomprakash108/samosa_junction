package com.samosajunction.cart.service;

import com.samosajunction.cart.config.CartProperties;
import com.samosajunction.cart.store.CartItemData;
import com.samosajunction.cart.store.CartPayload;
import com.samosajunction.cart.store.CartStore;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.entity.Inventory;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.product.entity.Category;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartStore cartStore;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    private CartService cartService;
    private UUID userId;
    private UUID productId;
    private Product paneer;

    @BeforeEach
    void setUp() {
        cartService = new CartService(
                cartStore,
                productRepository,
                inventoryRepository,
                new CartProperties(Duration.ofDays(7), 20, "cart:user:")
        );
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        paneer = new Product(
                "Paneer Samosa",
                "Filled pastry",
                new Category("PROTEIN", "High Protein"),
                4000,
                310,
                new BigDecimal("12.00"),
                SpiceLevel.MILD,
                true,
                Set.of("paneer"),
                Set.of("VEGETARIAN")
        );
        ReflectionTestUtils.setField(paneer, "id", productId);
    }

    @Test
    void addItemCreatesLineAndComputesSubtotal() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(paneer));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(new Inventory(productId, 50)));
        when(cartStore.load(userId)).thenReturn(CartPayload.empty(), new CartPayload(List.of(new CartItemData(productId, 2))));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(paneer));

        var cart = cartService.addItem(userId, productId, 2);

        verify(cartStore).save(eq(userId), any(CartPayload.class));
        assertThat(cart.totalQuantity()).isEqualTo(2);
        assertThat(cart.subtotal()).isEqualByComparingTo("80.00");
        assertThat(cart.items().getFirst().name()).isEqualTo("Paneer Samosa");
    }

    @Test
    void addItemIncrementsExistingQuantity() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(paneer));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(new Inventory(productId, 50)));
        when(cartStore.load(userId)).thenReturn(
                new CartPayload(List.of(new CartItemData(productId, 2))),
                new CartPayload(List.of(new CartItemData(productId, 3)))
        );
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(paneer));

        cartService.addItem(userId, productId, 1);

        verify(cartStore).save(userId, new CartPayload(List.of(new CartItemData(productId, 3))));
    }

    @Test
    void addItemRejectsUnavailableProduct() {
        ReflectionTestUtils.setField(paneer, "available", false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(paneer));

        assertThatThrownBy(() -> cartService.addItem(userId, productId, 1))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not available");
        verify(cartStore, never()).save(any(), any());
    }

    @Test
    void addItemRejectsUnknownProduct() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(userId, productId, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemRejectsQuantityAboveCap() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(paneer));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(new Inventory(productId, 50)));
        when(cartStore.load(userId)).thenReturn(CartPayload.empty());

        assertThatThrownBy(() -> cartService.addItem(userId, productId, 21))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(paneer));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(new Inventory(productId, 1)));
        when(cartStore.load(userId)).thenReturn(CartPayload.empty());

        assertThatThrownBy(() -> cartService.addItem(userId, productId, 3))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Only 1 left");
        verify(cartStore, never()).save(any(), any());
    }

    @Test
    void requireCheckoutItemsRejectsEmptyCart() {
        when(cartStore.load(userId)).thenReturn(CartPayload.empty());

        assertThatThrownBy(() -> cartService.requireCheckoutItems(userId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void requireCheckoutItemsReturnsCopyOfLines() {
        when(cartStore.load(userId)).thenReturn(new CartPayload(List.of(new CartItemData(productId, 2))));

        var lines = cartService.requireCheckoutItems(userId);

        assertThat(lines).containsExactly(new CartItemData(productId, 2));
    }

    @Test
    void updateQuantityZeroDeletesCartWhenLastItem() {
        when(cartStore.load(userId)).thenReturn(
                new CartPayload(List.of(new CartItemData(productId, 2))),
                CartPayload.empty()
        );

        var cart = cartService.updateItem(userId, productId, 0);

        verify(cartStore).delete(userId);
        verify(cartStore, never()).save(any(), any());
        assertThat(cart.items()).isEmpty();
    }
}
