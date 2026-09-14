package com.samosajunction.order.service;

import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private KitchenService kitchenService;
    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        kitchenService = new KitchenService(orderRepository, domainEventPublisher);
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void advancesConfirmedToPreparing() {
        Order order = confirmedOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        var response = kitchenService.advance(orderId, OrderStatus.PREPARING);

        assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.ORDER_PREPARING
        ));
    }

    @Test
    void rejectsSkippingToReady() {
        Order order = confirmedOrder();
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> kitchenService.advance(orderId, OrderStatus.READY))
                .isInstanceOf(InvalidRequestException.class);
    }

    private Order confirmedOrder() {
        Order order = new Order(userId, "Ada Lovelace", "42 Baker Street", "Mumbai", "MH", "400001", "order-1");
        order.addItem(new OrderItem(UUID.randomUUID(), "Paneer Samosa", 4000, 1));
        order.confirm();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }
}
