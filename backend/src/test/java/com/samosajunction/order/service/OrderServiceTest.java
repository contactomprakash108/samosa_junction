package com.samosajunction.order.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.cart.service.CartService;
import com.samosajunction.cart.store.CartItemData;
import com.samosajunction.common.exception.InsufficientFundsException;
import com.samosajunction.common.exception.InsufficientStockException;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.order.dto.CreateOrderRequest;
import com.samosajunction.order.dto.DeliveryAddressRequest;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.payment.entity.Payment;
import com.samosajunction.payment.entity.PaymentMethod;
import com.samosajunction.payment.entity.PaymentStatus;
import com.samosajunction.payment.service.PaymentService;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.user.service.UserService;
import com.samosajunction.wallet.dto.WalletResponse;
import com.samosajunction.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private UserService userService;

    private OrderService orderService;
    private UUID userId;
    private UUID productId;
    private UUID orderId;
    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productRepository,
                cartService,
                inventoryService,
                walletService,
                paymentService,
                domainEventPublisher,
                userService
        );
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        request = new CreateOrderRequest(new DeliveryAddressRequest(
                "Ada Lovelace",
                "42 Baker Street",
                "Mumbai",
                "MH",
                "400001"
        ));
    }

    @Test
    void createReservesDebitsAndClearsCart() {
        Product product = availableProduct(4000);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 2)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });
        when(walletService.debit(eq(userId), eq(8000), eq("order-debit:order-1"), eq(orderId.toString())))
                .thenReturn(new WalletResponse(UUID.randomUUID(), BigDecimal.ZERO, "INR"));

        var result = orderService.create(userId, request, "order-1");

        assertThat(result.replayed()).isFalse();
        assertThat(result.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.order().total()).isEqualByComparingTo("80.00");
        assertThat(result.order().items()).hasSize(1);
        verify(inventoryService).reserve(productId, 2);
        verify(paymentService).captureWallet(orderId, userId, 8000, "order-pay:" + userId + ":order-1");
        verify(cartService).clear(userId);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.ORDER_CREATED && event.aggregateId().equals(orderId)
        ));
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.ORDER_CONFIRMED
        ));
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.PAYMENT_SUCCESS
        ));
    }

    @Test
    void createWithCardDoesNotDebitWallet() {
        Product product = availableProduct(4000);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-card"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 2)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });

        var result = orderService.create(
                userId,
                new CreateOrderRequest(request.delivery(), true, PaymentMethod.CARD),
                "order-card"
        );

        assertThat(result.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(paymentService).captureCard(orderId, userId, 8000, "order-pay:" + userId + ":order-card");
        verify(inventoryService).reserve(productId, 2);
    }

    @Test
    void createCodReservesAndConfirmsWithoutCapturingMoney() {
        Product product = availableProduct(4000);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-cod"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 2)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });

        var result = orderService.create(
                userId,
                new CreateOrderRequest(request.delivery(), false, PaymentMethod.COD),
                "order-cod"
        );

        assertThat(result.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventoryService).reserve(productId, 2);
        verify(paymentService).initiateCod(orderId, userId, 8000, "order-pay:" + userId + ":order-cod");
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(paymentService, never()).captureWallet(any(), any(), anyInt(), anyString());
        verify(domainEventPublisher, never()).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.PAYMENT_SUCCESS
        ));
    }

    @Test
    void createReplaysExistingIdempotencyKey() {
        Order existing = new Order(userId, "Ada", "1 Road", "Pune", "MH", "411001", "order-1");
        existing.addItem(new OrderItem(productId, "Paneer Samosa", 4000, 1));
        existing.confirm();
        ReflectionTestUtils.setField(existing, "id", orderId);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.of(existing));

        var result = orderService.create(userId, request, "order-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.order().id()).isEqualTo(orderId);
        verify(cartService, never()).requireCheckoutItems(any());
        verify(inventoryService, never()).reserve(any(), anyInt());
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(cartService, never()).clear(any());
        verify(domainEventPublisher, never()).publishAfterCommit(any(DomainEvent.class));
    }

    @Test
    void createRejectsEmptyCart() {
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenThrow(new InvalidRequestException("Cart is empty"));

        assertThatThrownBy(() -> orderService.create(userId, request, "order-1"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty");
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnavailableProduct() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(product.getName()).thenReturn("Paneer Samosa");
        when(product.isAvailable()).thenReturn(false);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 1)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.create(userId, request, "order-1"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not available");
        verify(inventoryService, never()).reserve(any(), anyInt());
    }

    @Test
    void createPropagatesInsufficientStock() {
        Product product = availableProduct(4000);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 8)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });
        when(inventoryService.reserve(productId, 8)).thenThrow(new InsufficientStockException("Only 2 items left"));

        assertThatThrownBy(() -> orderService.create(userId, request, "order-1"))
                .isInstanceOf(InsufficientStockException.class);
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(cartService, never()).clear(any());
    }

    @Test
    void createPropagatesInsufficientFunds() {
        Product product = availableProduct(4000);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 1)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });
        when(walletService.debit(eq(userId), eq(4000), anyString(), anyString()))
                .thenThrow(new InsufficientFundsException("Wallet balance is insufficient"));

        assertThatThrownBy(() -> orderService.create(userId, request, "order-1"))
                .isInstanceOf(InsufficientFundsException.class);
        verify(paymentService, never()).captureWallet(any(), any(), anyInt(), anyString());
        verify(cartService, never()).clear(any());
    }

    @Test
    void getHidesAnotherUsersOrder() {
        Order other = new Order(UUID.randomUUID(), "Other", "1 Road", "Pune", "MH", "411001", "k1");
        ReflectionTestUtils.setField(other, "id", orderId);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> orderService.get(userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
    }

    @Test
    void createLeavesOrderUnpaidWhenPayNowFalse() {
        Product product = availableProduct(4000);
        when(orderRepository.findWithItemsByUserIdAndIdempotencyKey(userId, "order-1"))
                .thenReturn(Optional.empty());
        when(cartService.requireCheckoutItems(userId)).thenReturn(List.of(new CartItemData(productId, 2)));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });

        var result = orderService.create(userId, new CreateOrderRequest(request.delivery(), false), "order-1");

        assertThat(result.order().status()).isEqualTo(OrderStatus.CREATED);
        verify(inventoryService, never()).reserve(any(), anyInt());
        verify(walletService, never()).debit(any(), anyInt(), anyString(), anyString());
        verify(paymentService, never()).captureWallet(any(), any(), anyInt(), anyString());
        verify(cartService).clear(userId);
    }

    @Test
    void cancelReleasesStockAndRefundsWallet() {
        Order order = confirmedOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentService.findByOrderId(orderId)).thenReturn(Optional.of(successfulPayment()));

        var response = orderService.cancel(userId, orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).release(productId, 2);
        verify(walletService).refund(userId, 8000, "order-refund:" + orderId, orderId.toString());
        verify(paymentService).refundForOrder(orderId);
    }

    @Test
    void cancelCardPaymentDoesNotRefundWallet() {
        Order order = confirmedOrder();
        Payment payment = new Payment(orderId, userId, 8000, PaymentMethod.CARD, "pay-card");
        payment.markSuccess();
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentService.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        var response = orderService.cancel(userId, orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(walletService, never()).refund(any(), anyInt(), anyString(), anyString());
        verify(paymentService).refundForOrder(orderId);
    }

    @Test
    void cancelUnpaidCreatedOrderDoesNotReleaseOrRefund() {
        Order order = new Order(userId, "Ada Lovelace", "42 Baker Street", "Mumbai", "MH", "400001", "order-1");
        order.addItem(new OrderItem(productId, "Paneer Samosa", 4000, 2));
        ReflectionTestUtils.setField(order, "id", orderId);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentService.findByOrderId(orderId)).thenReturn(Optional.empty());

        var response = orderService.cancel(userId, orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService, never()).release(any(), anyInt());
        verify(walletService, never()).refund(any(), anyInt(), anyString(), anyString());
    }

    @Test
    void cancelRejectsAlreadyCancelled() {
        Order order = confirmedOrder();
        order.cancel();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(userId, orderId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already cancelled");
        verify(inventoryService, never()).release(any(), anyInt());
    }

    @Test
    void refundPaymentReplaysWhenAlreadyRefunded() {
        Payment payment = successfulPayment();
        payment.markRefunded();
        UUID paymentId = payment.getId();
        when(paymentService.requireById(paymentId)).thenReturn(payment);

        var response = orderService.refundPayment(userId, paymentId, false);

        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        verify(inventoryService, never()).release(any(), anyInt());
    }

    @Test
    void cancelRejectsDeliveredOrder() {
        Order order = confirmedOrder();
        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(userId, orderId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("DELIVERED");
        verify(walletService, never()).refund(any(), anyInt(), anyString(), anyString());
    }

    @Test
    void staffRefundOnDeliveredCreditsWalletWithoutCancelling() {
        Order order = confirmedOrder();
        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);
        Payment payment = successfulPayment();
        UUID paymentId = payment.getId();
        when(paymentService.requireById(paymentId)).thenReturn(payment);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(paymentService.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        var response = orderService.refundPayment(UUID.randomUUID(), paymentId, true);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(walletService).refund(userId, 8000, "order-refund:" + orderId, orderId.toString());
        verify(paymentService).refundForOrder(orderId);
        verify(inventoryService, never()).release(any(), anyInt());
        assertThat(response).isNotNull();
    }

    private Payment successfulPayment() {
        Payment payment = new Payment(orderId, userId, 8000, PaymentMethod.WALLET, "pay-1");
        payment.markSuccess();
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        return payment;
    }

    private Order confirmedOrder() {
        Order order = new Order(userId, "Ada Lovelace", "42 Baker Street", "Mumbai", "MH", "400001", "order-1");
        order.addItem(new OrderItem(productId, "Paneer Samosa", 4000, 2));
        order.confirm();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private Product availableProduct(int pricePaise) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(product.getName()).thenReturn("Paneer Samosa");
        when(product.getPricePaise()).thenReturn(pricePaise);
        when(product.isAvailable()).thenReturn(true);
        return product;
    }
}
