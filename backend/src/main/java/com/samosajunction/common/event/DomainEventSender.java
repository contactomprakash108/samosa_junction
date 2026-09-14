package com.samosajunction.common.event;

public interface DomainEventSender {

    void send(DomainEvent event);
}
