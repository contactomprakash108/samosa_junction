package com.samosajunction.payment.service;

import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.payment.dto.CreatePaymentRequest;
import com.samosajunction.payment.entity.Payment;
import com.samosajunction.payment.entity.PaymentMethod;
import com.samosajunction.payment.entity.PaymentStatus;
import com.samosajunction.payment.repository.PaymentRepository;
import com.samosajunction.wallet.dto.WalletResponse;
import com.samosajunction.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private WalletService walletService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PaymentService paymentService;
    private UUID userId;
    private UUID orderId;
    private UUID productId;
    private UUID paymentId;
    private CreatePaymentRequest request;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                orderRepository,
                inventoryService,
                walletService,
                domainEventPublisher
        );
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        request = new CreatePaymentRequest(orderId, PaymentMethod.WALLET, false);
    }

    @Test
    void payDebitsReservesAndConfirmsCreatedOrder() {
        Order order = createdOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByIdempotencyKey("pay-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", paymentId);
            return payment;
        });
        when(walletService.getWallet(userId)).thenReturn(new WalletResponse(UUID.randomUUID(), new BigDecimal("100.00"), "INR"));

        var result = paymentService.pay(userId, request, "pay-1");

        assertThat(result.replayed()).isFalse();
        assertThat(result.failed()).isFalse();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(walletService).debit(eq(userId), eq(8000), eq("pay:pay-1"), eq(orderId.toString()));
        verify(inventoryService).reserve(productId, 2);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.PAYMENT_SUCCESS
        ));
    }

    @Test
    void payWithCardDoesNotDebitWallet() {
        Order order = createdOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByIdempotencyKey("pay-card")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", paymentId);
            return payment;
        });

        var result = paymentService.pay(userId, new CreatePaymentRequest(orderId, PaymentMethod.CARD, false), "pay-card");

        assertThat(result.failed()).isFalse();
        assertThat(result.payment().method()).isEqualTo(PaymentMethod.CARD);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(inventoryService).reserve(productId, 2);
    }

    @Test
    void payReplaysSuccessfulPayment() {
        Order order = createdOrder();
        Payment existing = successfulPayment();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByIdempotencyKey("pay-1")).thenReturn(Optional.of(existing));

        var result = paymentService.pay(userId, request, "pay-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(inventoryService, never()).reserve(any(), anyInt());
        verify(domainEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void payPersistsFailedOnSimulatedDecline() {
        Order order = createdOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByIdempotencyKey("pay-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", paymentId);
            return payment;
        });

        var result = paymentService.pay(userId, new CreatePaymentRequest(orderId, PaymentMethod.WALLET, true), "pay-1");

        assertThat(result.failed()).isTrue();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.payment().failureReason()).isEqualTo("Simulated payment decline");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.PAYMENT_FAILED
        ));
    }

    @Test
    void payPersistsFailedWhenWalletIsShort() {
        Order order = createdOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByIdempotencyKey("pay-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.getWallet(userId)).thenReturn(new WalletResponse(UUID.randomUUID(), new BigDecimal("10.00"), "INR"));

        var result = paymentService.pay(userId, request, "pay-1");

        assertThat(result.failed()).isTrue();
        assertThat(result.payment().failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
    }

    @Test
    void payRetriesFailedPayment() {
        Order order = createdOrder();
        Payment failed = new Payment(orderId, userId, 8000, PaymentMethod.WALLET, "pay-1");
        failed.markFailed("INSUFFICIENT_FUNDS");
        ReflectionTestUtils.setField(failed, "id", paymentId);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByIdempotencyKey("pay-1")).thenReturn(Optional.of(failed));
        when(walletService.getWallet(userId)).thenReturn(new WalletResponse(UUID.randomUUID(), new BigDecimal("100.00"), "INR"));

        var result = paymentService.pay(userId, request, "pay-1");

        assertThat(result.failed()).isFalse();
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(walletService).debit(eq(userId), eq(8000), eq("pay:pay-1"), eq(orderId.toString()));
    }

    @Test
    void getHidesAnotherUsersPayment() {
        Payment payment = successfulPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.get(UUID.randomUUID(), paymentId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }

    @Test
    void getAllowsStaffToReadAnyPayment() {
        Payment payment = successfulPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        var response = paymentService.get(UUID.randomUUID(), paymentId, true);

        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void collectCodMarksInitiatedPaymentSuccessAfterDelivery() {
        Order order = createdOrder();
        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);
        Payment payment = new Payment(orderId, userId, 8000, PaymentMethod.COD, "order-pay-cod");
        ReflectionTestUtils.setField(payment, "id", paymentId);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        var response = paymentService.collectCod(orderId);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.PAYMENT_SUCCESS
        ));
    }

    @Test
    void collectCodRejectsBeforeDelivery() {
        Order order = createdOrder();
        order.confirm();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.collectCod(orderId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("delivered");
    }

    private Order createdOrder() {
        Order order = new Order(userId, "Ada Lovelace", "42 Baker Street", "Mumbai", "MH", "400001", "order-1");
        order.addItem(new OrderItem(productId, "Paneer Samosa", 4000, 2));
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private Payment successfulPayment() {
        Payment payment = new Payment(orderId, userId, 8000, PaymentMethod.WALLET, "pay-1");
        payment.markSuccess();
        ReflectionTestUtils.setField(payment, "id", paymentId);
        return payment;
    }
}
