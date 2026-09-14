package com.samosajunction.order.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.cart.service.CartService;
import com.samosajunction.cart.store.CartItemData;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.order.dto.CheckoutResult;
import com.samosajunction.order.dto.CreateOrderRequest;
import com.samosajunction.order.dto.DeliveryAddressRequest;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.payment.dto.PaymentResponse;
import com.samosajunction.payment.entity.Payment;
import com.samosajunction.payment.entity.PaymentMethod;
import com.samosajunction.payment.entity.PaymentStatus;
import com.samosajunction.payment.service.PaymentService;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.wallet.service.WalletService;
import com.samosajunction.user.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final WalletService walletService;
    private final PaymentService paymentService;
    private final DomainEventPublisher domainEventPublisher;
    private final UserService userService;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CartService cartService,
            InventoryService inventoryService,
            WalletService walletService,
            PaymentService paymentService,
            DomainEventPublisher domainEventPublisher,
            UserService userService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.inventoryService = inventoryService;
        this.walletService = walletService;
        this.paymentService = paymentService;
        this.domainEventPublisher = domainEventPublisher;
        this.userService = userService;
    }

    @Transactional
    public CheckoutResult create(UUID userId, CreateOrderRequest request, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        var existing = orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            return new CheckoutResult(OrderResponse.from(existing.get()), true);
        }

        List<CartItemData> cartItems = cartService.requireCheckoutItems(userId);
        Map<UUID, Product> products = productRepository.findAllById(
                cartItems.stream().map(CartItemData::productId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        DeliveryAddressRequest delivery = request.delivery();
        Order order = new Order(
                userId,
                delivery.recipientName(),
                delivery.line1(),
                delivery.city(),
                delivery.state(),
                delivery.pincode(),
                key
        );
        for (CartItemData line : cartItems) {
            Product product = products.get(line.productId());
            if (product == null) {
                throw new InvalidRequestException("A cart product is no longer in the catalog");
            }
            if (!product.isAvailable()) {
                throw new InvalidRequestException("Product is not available: " + product.getName());
            }
            order.addItem(new OrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPricePaise(),
                    line.quantity()
            ));
        }

        try {
            orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException ex) {
            Order raced = orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, key)
                    .orElseThrow(() -> ex);
            return new CheckoutResult(OrderResponse.from(raced), true);
        }

        var method = request.resolvedPaymentMethod();
        if (method == PaymentMethod.COD) {
            for (OrderItem item : order.getItems()) {
                inventoryService.reserve(item.getProductId(), item.getQuantity());
            }
            paymentService.initiateCod(order.getId(), userId, order.getTotalPaise(), "order-pay:" + userId + ":" + key);
            order.confirm();
        } else if (request.shouldPayNow()) {
            for (OrderItem item : order.getItems()) {
                inventoryService.reserve(item.getProductId(), item.getQuantity());
            }
            String orderId = order.getId().toString();
            if (method == PaymentMethod.CARD) {
                paymentService.captureCard(order.getId(), userId, order.getTotalPaise(), "order-pay:" + userId + ":" + key);
            } else {
                walletService.debit(userId, order.getTotalPaise(), "order-debit:" + key, orderId);
                paymentService.captureWallet(order.getId(), userId, order.getTotalPaise(), "order-pay:" + userId + ":" + key);
            }
            order.confirm();
        }
        domainEventPublisher.publishAfterCommit(DomainEvent.of(DomainEventType.ORDER_CREATED, order.getId(), userId));
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            domainEventPublisher.publishAfterCommit(DomainEvent.of(DomainEventType.ORDER_CONFIRMED, order.getId(), userId));
            if (method != PaymentMethod.COD) {
                domainEventPublisher.publishAfterCommit(DomainEvent.of(DomainEventType.PAYMENT_SUCCESS, order.getId(), userId));
            }
        }
        clearCartAfterCommit(userId);
        rememberDeliveryAfterCommit(userId, delivery);
        return new CheckoutResult(OrderResponse.from(order), false);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(UUID userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID userId, UUID orderId) {
        return OrderResponse.from(requireOwnedOrder(userId, orderId));
    }

    @Transactional
    public OrderResponse cancel(UUID userId, UUID orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        cancelInternal(order);
        return OrderResponse.from(order);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID actorId, UUID paymentId, boolean privileged) {
        Payment payment = paymentService.requireById(paymentId);
        if (!privileged && !payment.getUserId().equals(actorId)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return PaymentResponse.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidRequestException("Only a successful payment can be refunded");
        }
        Order order = orderRepository.findWithItemsById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!privileged && !order.getUserId().equals(actorId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        if (order.getStatus().canCancel()) {
            cancelInternal(order);
        } else {
            if (!privileged) {
                throw new InvalidRequestException("Ask support to refund this order");
            }
            refundKeepOrder(order);
        }
        return PaymentResponse.from(paymentService.requireById(paymentId));
    }

    private void cancelInternal(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResourceConflictException("Order is already cancelled");
        }
        if (!order.getStatus().canCancel()) {
            throw new InvalidRequestException("Order cannot be cancelled in status " + order.getStatus());
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            for (OrderItem item : order.getItems()) {
                inventoryService.release(item.getProductId(), item.getQuantity());
            }
        }
        var payment = paymentService.findByOrderId(order.getId());
        if (payment.isPresent() && payment.get().getStatus() == PaymentStatus.SUCCESS) {
            if (payment.get().getMethod() == PaymentMethod.WALLET) {
                walletService.refund(
                        order.getUserId(),
                        order.getTotalPaise(),
                        "order-refund:" + order.getId(),
                        order.getId().toString()
                );
            }
            paymentService.refundForOrder(order.getId());
        } else {
            paymentService.cancelIfUnpaid(order.getId());
        }
        order.cancel();
        domainEventPublisher.publishAfterCommit(
                DomainEvent.of(DomainEventType.ORDER_CANCELLED, order.getId(), order.getUserId())
        );
    }

    private void refundKeepOrder(Order order) {
        var payment = paymentService.findByOrderId(order.getId());
        if (payment.isEmpty() || payment.get().getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidRequestException("No successful payment to refund");
        }
        if (payment.get().getMethod() == PaymentMethod.WALLET) {
            walletService.refund(
                    order.getUserId(),
                    order.getTotalPaise(),
                    "order-refund:" + order.getId(),
                    order.getId().toString()
            );
        }
        paymentService.refundForOrder(order.getId());
    }

    private Order requireOwnedOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    private void clearCartAfterCommit(UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cartService.clear(userId);
                }
            });
            return;
        }
        cartService.clear(userId);
    }

    private void rememberDeliveryAfterCommit(UUID userId, DeliveryAddressRequest delivery) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    userService.rememberDelivery(userId, delivery);
                }
            });
            return;
        }
        userService.rememberDelivery(userId, delivery);
    }

    private static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidRequestException("Idempotency-Key header is required");
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 128) {
            throw new InvalidRequestException("Idempotency-Key is too long");
        }
        return trimmed;
    }
}
