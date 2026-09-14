package com.samosajunction.common.event;

import com.samosajunction.common.support.AfterCommit;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@Primary
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final DomainEventSender sender;

    public OutboxDomainEventPublisher(OutboxEventRepository outboxEventRepository, DomainEventSender sender) {
        this.outboxEventRepository = outboxEventRepository;
        this.sender = sender;
    }

    @Override
    @Transactional
    public void publishAfterCommit(DomainEvent event) {
        if (outboxEventRepository.findByEventId(event.eventId()).isEmpty()) {
            outboxEventRepository.save(new OutboxEvent(event));
        }
        AfterCommit.run(() -> dispatch(event.eventId()));
    }

    @Transactional
    public void dispatch(UUID eventId) {
        OutboxEvent row = outboxEventRepository.findByEventId(eventId).orElse(null);
        if (row == null || "PUBLISHED".equals(row.getStatus())) {
            return;
        }
        sender.send(row.toDomainEvent());
        row.markPublished(Instant.now());
    }

    @Transactional
    public int publishPending() {
        int sent = 0;
        for (OutboxEvent row : outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")) {
            sender.send(row.toDomainEvent());
            row.markPublished(Instant.now());
            sent++;
        }
        return sent;
    }
}
