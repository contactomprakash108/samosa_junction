package com.samosajunction.common.event;

public interface DomainEventPublisher {

    void publishAfterCommit(DomainEvent event);
}
