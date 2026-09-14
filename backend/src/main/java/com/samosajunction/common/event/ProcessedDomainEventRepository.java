package com.samosajunction.common.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedDomainEventRepository extends JpaRepository<ProcessedDomainEvent, ProcessedDomainEvent.Key> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);
}
