package com.samosajunction.inventory.service;

import com.samosajunction.common.exception.InsufficientStockException;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.dto.InventoryResponse;
import com.samosajunction.inventory.entity.Inventory;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        return toResponse(getRequired(productId));
    }

    @Transactional
    public InventoryResponse setQuantity(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new InvalidRequestException("Quantity cannot be negative");
        }
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseGet(() -> inventoryRepository.save(new Inventory(productId, 0)));
        inventory.setQuantity(quantity);
        return toResponse(inventory);
    }

    @Transactional
    public void createForNewProduct(UUID productId) {
        if (inventoryRepository.existsById(productId)) {
            return;
        }
        inventoryRepository.save(new Inventory(productId, 0));
    }

    /**
     * Decrement stock inside the caller's transaction (checkout later).
     * Concurrent updates fail with OptimisticLockingFailureException → HTTP 409.
     */
    @Transactional
    public InventoryResponse reserve(UUID productId, int quantity) {
        requirePositive(quantity);
        Inventory inventory = getRequired(productId);
        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Only %d items left for this product".formatted(inventory.getQuantity())
            );
        }
        inventory.decrease(quantity);
        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse release(UUID productId, int quantity) {
        requirePositive(quantity);
        Inventory inventory = getRequired(productId);
        inventory.increase(quantity);
        return toResponse(inventory);
    }

    private Inventory getRequired(UUID productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
    }

    private static void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new InvalidRequestException("Quantity must be at least 1");
        }
    }

    private static InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getVersion(),
                inventory.getUpdatedAt()
        );
    }
}
