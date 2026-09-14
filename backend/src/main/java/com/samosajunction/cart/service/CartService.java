package com.samosajunction.cart.service;

import com.samosajunction.cart.config.CartProperties;
import com.samosajunction.cart.dto.CartItemResponse;
import com.samosajunction.cart.dto.CartResponse;
import com.samosajunction.cart.store.CartItemData;
import com.samosajunction.cart.store.CartPayload;
import com.samosajunction.cart.store.CartStore;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.product.support.InrMoney;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartStore cartStore;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartProperties cartProperties;

    public CartService(
            CartStore cartStore,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CartProperties cartProperties
    ) {
        this.cartStore = cartStore;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.cartProperties = cartProperties;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        return toResponse(cartStore.load(userId));
    }

    public List<CartItemData> requireCheckoutItems(UUID userId) {
        CartPayload payload = cartStore.load(userId);
        if (payload.isEmpty()) {
            throw new InvalidRequestException("Cart is empty");
        }
        return List.copyOf(payload.items());
    }

    public CartResponse addItem(UUID userId, UUID productId, int quantity) {
        Product product = requireAvailableProduct(productId);
        CartPayload current = cartStore.load(userId);
        int existing = current.find(productId).map(CartItemData::quantity).orElse(0);
        int next = existing + quantity;
        requireStock(product.getId(), next);
        persist(userId, upsert(current, product.getId(), next));
        return toResponse(cartStore.load(userId));
    }

    public CartResponse updateItem(UUID userId, UUID productId, int quantity) {
        if (quantity == 0) {
            return removeItem(userId, productId);
        }
        requireAvailableProduct(productId);
        requireStock(productId, quantity);
        CartPayload current = cartStore.load(userId);
        persist(userId, upsert(current, productId, quantity));
        return toResponse(cartStore.load(userId));
    }

    public CartResponse removeItem(UUID userId, UUID productId) {
        CartPayload current = cartStore.load(userId);
        persist(userId, new CartPayload(
                current.items().stream().filter(item -> !item.productId().equals(productId)).toList()
        ));
        return toResponse(cartStore.load(userId));
    }

    public void clear(UUID userId) {
        cartStore.delete(userId);
    }

    private void persist(UUID userId, CartPayload payload) {
        if (payload.isEmpty()) {
            cartStore.delete(userId);
            return;
        }
        cartStore.save(userId, payload);
    }

    private CartPayload upsert(CartPayload current, UUID productId, int quantity) {
        int capped = cap(quantity);
        List<CartItemData> items = new ArrayList<>();
        boolean replaced = false;
        for (CartItemData item : current.items()) {
            if (item.productId().equals(productId)) {
                items.add(new CartItemData(productId, capped));
                replaced = true;
            } else {
                items.add(item);
            }
        }
        if (!replaced) {
            items.add(new CartItemData(productId, capped));
        }
        return new CartPayload(items);
    }

    private int cap(int quantity) {
        if (quantity < 1) {
            throw new InvalidRequestException("Quantity must be at least 1");
        }
        if (quantity > cartProperties.maxQuantity()) {
            throw new InvalidRequestException(
                    "Quantity cannot exceed %d per product".formatted(cartProperties.maxQuantity())
            );
        }
        return quantity;
    }

    private Product requireAvailableProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!product.isAvailable()) {
            throw new InvalidRequestException("Product is not available");
        }
        return product;
    }

    private void requireStock(UUID productId, int quantity) {
        int stock = stockOf(productId);
        if (stock <= 0) {
            throw new InvalidRequestException("Product is sold out");
        }
        if (quantity > stock) {
            throw new InvalidRequestException("Only " + stock + " left in stock");
        }
    }

    private int stockOf(UUID productId) {
        return inventoryRepository.findById(productId)
                .map(com.samosajunction.inventory.entity.Inventory::getQuantity)
                .orElse(0);
    }

    private CartResponse toResponse(CartPayload payload) {
        if (payload.isEmpty()) {
            return new CartResponse(List.of(), 0, InrMoney.toRupees(0));
        }
        var products = productRepository.findAllById(
                payload.items().stream().map(CartItemData::productId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemResponse> lines = payload.items().stream()
                .map(item -> toLine(item, products))
                .toList();
        int totalQuantity = lines.stream().mapToInt(CartItemResponse::quantity).sum();
        int subtotalPaise = lines.stream()
                .filter(line -> !line.productMissing() && line.available())
                .mapToInt(line -> InrMoney.toPaise(line.lineTotal()))
                .sum();
        return new CartResponse(lines, totalQuantity, InrMoney.toRupees(subtotalPaise));
    }

    private CartItemResponse toLine(CartItemData item, Map<UUID, Product> products) {
        Product product = products.get(item.productId());
        if (product == null) {
            return new CartItemResponse(
                    item.productId(),
                    "Unavailable product",
                    item.quantity(),
                    InrMoney.toRupees(0),
                    InrMoney.toRupees(0),
                    false,
                    true,
                    0
            );
        }
        BigDecimal unit = InrMoney.toRupees(product.getPricePaise());
        BigDecimal lineTotal = InrMoney.toRupees(product.getPricePaise() * item.quantity());
        return new CartItemResponse(
                product.getId(),
                product.getName(),
                item.quantity(),
                unit,
                lineTotal,
                product.isAvailable(),
                false,
                stockOf(product.getId())
        );
    }
}
