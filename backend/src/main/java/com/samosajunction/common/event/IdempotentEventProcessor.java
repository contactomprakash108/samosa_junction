package com.samosajunction.common.event;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IdempotentEventProcessor {

    private final ProcessedDomainEventRepository processedDomainEventRepository;

    public IdempotentEventProcessor(ProcessedDomainEventRepository processedDomainEventRepository) {
        this.processedDomainEventRepository = processedDomainEventRepository;
    }

    @Transactional
    public boolean claim(UUID eventId, String consumerName) {
        if (processedDomainEventRepository.existsByEventIdAndConsumerName(eventId, consumerName)) {
            return false;
        }
        try {
            processedDomainEventRepository.saveAndFlush(new ProcessedDomainEvent(eventId, consumerName));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
