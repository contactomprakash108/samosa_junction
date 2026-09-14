package com.samosajunction.inventory.service;

import com.samosajunction.common.exception.InsufficientStockException;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.inventory.entity.Inventory;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    private InventoryService inventoryService;
    private UUID productId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, productRepository);
        productId = UUID.randomUUID();
        inventory = new Inventory(productId, 10);
    }

    @Test
    void reserveDecreasesStock() {
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        var result = inventoryService.reserve(productId, 8);

        assertThat(result.quantity()).isEqualTo(2);
        assertThat(inventory.getQuantity()).isEqualTo(2);
    }

    @Test
    void reserveRejectsOversell() {
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserve(productId, 11))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Only 10");
        assertThat(inventory.getQuantity()).isEqualTo(10);
    }

    @Test
    void twoSequentialReservesCannotExceedStock() {
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        inventoryService.reserve(productId, 8);
        assertThatThrownBy(() -> inventoryService.reserve(productId, 8))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(inventory.getQuantity()).isEqualTo(2);
    }

    @Test
    void releaseReturnsStock() {
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        inventoryService.reserve(productId, 3);

        var result = inventoryService.release(productId, 3);

        assertThat(result.quantity()).isEqualTo(10);
    }

    @Test
    void reserveRejectsZeroQuantity() {
        assertThatThrownBy(() -> inventoryService.reserve(productId, 0))
                .isInstanceOf(InvalidRequestException.class);
    }
}
