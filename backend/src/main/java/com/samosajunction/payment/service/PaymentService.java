package com.samosajunction.payment.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.payment.dto.CreatePaymentRequest;
import com.samosajunction.payment.dto.PayResult;
import com.samosajunction.payment.dto.PaymentResponse;
import com.samosajunction.payment.entity.Payment;
import com.samosajunction.payment.entity.PaymentMethod;
import com.samosajunction.payment.entity.PaymentStatus;
import com.samosajunction.payment.repository.PaymentRepository;
import com.samosajunction.product.support.InrMoney;
import com.samosajunction.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


/**
 * Simulated WALLET and CARD payments. CARD does not call a bank or Razorpay/Stripe.
 * Declines are committed as FAILED so GET and retry work. Do not throw after
 * markFailed — a RuntimeException would roll the FAILED row back.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final WalletService walletService;
    private final DomainEventPublisher domainEventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            InventoryService inventoryService,
            WalletService walletService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.walletService = walletService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public Payment captureWallet(UUID orderId, UUID userId, int amountPaise, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    var payment = new Payment(orderId, userId, amountPaise, PaymentMethod.WALLET, idempotencyKey);
                    payment.markSuccess();
                    return paymentRepository.save(payment);
                });
    }

    @Transactional
    public Payment captureCard(UUID orderId, UUID userId, int amountPaise, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    var payment = new Payment(orderId, userId, amountPaise, PaymentMethod.CARD, idempotencyKey);
                    payment.markSuccess();
                    return paymentRepository.save(payment);
                });
    }

    @Transactional
    public Payment initiateCod(UUID orderId, UUID userId, int amountPaise, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> paymentRepository.save(
                        new Payment(orderId, userId, amountPaise, PaymentMethod.COD, idempotencyKey)
                ));
    }

    @Transactional
    public PaymentResponse collectCod(UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidRequestException("Collect cash after the order is delivered");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getMethod() != PaymentMethod.COD) {
            throw new InvalidRequestException("This order is not cash on delivery");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResponse.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new InvalidRequestException("Cash cannot be collected in status " + payment.getStatus());
        }
        payment.markSuccess();
        domainEventPublisher.publishAfterCommit(
                DomainEvent.of(DomainEventType.PAYMENT_SUCCESS, order.getId(), order.getUserId())
        );
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void cancelIfUnpaid(UUID orderId) {
        paymentRepository.findByOrderId(orderId)
                .filter(payment -> payment.getStatus() == PaymentStatus.INITIATED)
                .ifPresent(payment -> payment.markFailed("Order cancelled"));
    }

    @Transactional
    public Payment refundForOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(payment -> {
                    if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        payment.markRefunded();
                    }
                    return payment;
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Payment requireById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID actorId, UUID paymentId, boolean privileged) {
        Payment payment = requireById(paymentId);
        if (!privileged && !payment.getUserId().equals(actorId)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PaymentResponse::from);
    }

    @Transactional
    public PayResult pay(UUID userId, CreatePaymentRequest request, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        if (request.resolvedMethod() == PaymentMethod.COD) {
            throw new InvalidRequestException("Cash on delivery is collected by staff after delivery");
        }
        if (request.resolvedMethod() != PaymentMethod.WALLET && request.resolvedMethod() != PaymentMethod.CARD) {
            throw new InvalidRequestException("Unsupported payment method");
        }

        Order order = requireOwnedOrder(userId, request.orderId());

        var byKey = paymentRepository.findByIdempotencyKey(key);
        if (byKey.isPresent()) {
            Payment existing = byKey.get();
            if (!existing.getOrderId().equals(order.getId()) || !existing.getUserId().equals(userId)) {
                throw new ResourceConflictException("Idempotency key was already used");
            }
            if (existing.getStatus() == PaymentStatus.SUCCESS || existing.getStatus() == PaymentStatus.REFUNDED) {
                return new PayResult(PaymentResponse.from(existing), true);
            }
            return new PayResult(attemptCapture(order, existing, request.shouldSimulateFailure()), false);
        }

        var byOrder = paymentRepository.findByOrderId(order.getId());
        if (byOrder.isPresent()) {
            Payment existing = byOrder.get();
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                return new PayResult(PaymentResponse.from(existing), true);
            }
            if (existing.getStatus() == PaymentStatus.REFUNDED) {
                throw new ResourceConflictException("Payment was already refunded");
            }
            return new PayResult(attemptCapture(order, existing, request.shouldSimulateFailure()), false);
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidRequestException("Order cannot be paid in status " + order.getStatus());
        }

        Payment payment = new Payment(order.getId(), userId, order.getTotalPaise(), request.resolvedMethod(), key);
        paymentRepository.saveAndFlush(payment);
        return new PayResult(attemptCapture(order, payment, request.shouldSimulateFailure()), false);
    }

    private PaymentResponse attemptCapture(Order order, Payment payment, boolean simulateFailure) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new InvalidRequestException("Order cannot be paid in status " + order.getStatus());
        }
        if (simulateFailure) {
            payment.markFailed("Simulated payment decline");
            publishPaymentFailed(order);
            return PaymentResponse.from(payment);
        }

        if (payment.getMethod() == PaymentMethod.WALLET) {
            int availablePaise = InrMoney.toPaise(walletService.getWallet(order.getUserId()).balance());
            if (availablePaise < order.getTotalPaise()) {
                payment.markFailed("INSUFFICIENT_FUNDS");
                publishPaymentFailed(order);
                return PaymentResponse.from(payment);
            }
            walletService.debit(
                    order.getUserId(),
                    order.getTotalPaise(),
                    "pay:" + payment.getIdempotencyKey(),
                    order.getId().toString()
            );
        }
        for (OrderItem item : order.getItems()) {
            inventoryService.reserve(item.getProductId(), item.getQuantity());
        }
        payment.markSuccess();
        order.confirm();
        domainEventPublisher.publishAfterCommit(
                DomainEvent.of(DomainEventType.ORDER_CONFIRMED, order.getId(), order.getUserId())
        );
        domainEventPublisher.publishAfterCommit(
                DomainEvent.of(DomainEventType.PAYMENT_SUCCESS, order.getId(), order.getUserId())
        );
        return PaymentResponse.from(payment);
    }

    private void publishPaymentFailed(Order order) {
        domainEventPublisher.publishAfterCommit(
                DomainEvent.of(DomainEventType.PAYMENT_FAILED, order.getId(), order.getUserId())
        );
    }

    private Order requireOwnedOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
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
