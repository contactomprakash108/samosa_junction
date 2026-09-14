package com.samosajunction.order.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class KitchenService {

    private static final Set<OrderStatus> KITCHEN = EnumSet.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;

    public KitchenService(OrderRepository orderRepository, DomainEventPublisher domainEventPublisher) {
        this.orderRepository = orderRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(Pageable pageable) {
        return orderRepository.findKitchenQueue(KITCHEN, pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse advance(UUID orderId, OrderStatus requested) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus next = order.getStatus().kitchenSuccessor();
        if (next == null || next != requested) {
            throw new InvalidRequestException(
                    "Kitchen can only move this order to " + (next == null ? "no further status" : next)
            );
        }
        order.moveTo(next);
        DomainEventType type = switch (next) {
            case PREPARING -> DomainEventType.ORDER_PREPARING;
            case READY -> DomainEventType.ORDER_READY;
            case OUT_FOR_DELIVERY -> DomainEventType.ORDER_OUT_FOR_DELIVERY;
            case DELIVERED -> DomainEventType.ORDER_DELIVERED;
            default -> null;
        };
        if (type != null) {
            domainEventPublisher.publishAfterCommit(DomainEvent.of(type, order.getId(), order.getUserId()));
        }
        return OrderResponse.from(order);
    }
}
