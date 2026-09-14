package com.samosajunction.common.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AfterCommitDomainEventPublisherTest {

    @Mock
    private DomainEventSender sender;

    @Test
    void sendsImmediatelyWhenNoTransactionIsActive() {
        var publisher = new AfterCommitDomainEventPublisher(sender);
        DomainEvent event = DomainEvent.of(DomainEventType.ORDER_CREATED, UUID.randomUUID(), UUID.randomUUID());

        publisher.publishAfterCommit(event);

        verify(sender).send(event);
    }
}
