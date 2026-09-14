package com.samosajunction.common.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private DomainEventSender sender;

    @Test
    void savesAndSendsWhenNoTransactionIsActive() {
        DomainEvent event = DomainEvent.of(DomainEventType.ORDER_CREATED, UUID.randomUUID(), UUID.randomUUID());
        when(outboxEventRepository.findByEventId(any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new OutboxEvent(event)));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var publisher = new OutboxDomainEventPublisher(outboxEventRepository, sender);

        publisher.publishAfterCommit(event);

        verify(sender).send(any(DomainEvent.class));
    }
}
