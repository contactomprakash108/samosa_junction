package com.samosajunction.common.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    void sameOrderAndTypeProduceTheSameEventId() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DomainEvent first = DomainEvent.of(DomainEventType.ORDER_CONFIRMED, orderId, userId);
        DomainEvent second = DomainEvent.of(DomainEventType.ORDER_CONFIRMED, orderId, userId);

        assertThat(first.eventId()).isEqualTo(second.eventId());
    }

    @Test
    void differentTypesForTheSameOrderProduceDifferentEventIds() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DomainEvent created = DomainEvent.of(DomainEventType.ORDER_CREATED, orderId, userId);
        DomainEvent confirmed = DomainEvent.of(DomainEventType.ORDER_CONFIRMED, orderId, userId);

        assertThat(created.eventId()).isNotEqualTo(confirmed.eventId());
    }
}
