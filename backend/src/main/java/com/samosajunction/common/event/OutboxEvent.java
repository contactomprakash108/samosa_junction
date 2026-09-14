package com.samosajunction.common.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private DomainEventType eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(DomainEvent event) {
        this.eventId = event.eventId();
        this.eventType = event.type();
        this.aggregateId = event.aggregateId();
        this.userId = event.userId();
        this.status = "PENDING";
        this.createdAt = event.occurredAt();
    }

    public DomainEvent toDomainEvent() {
        return new DomainEvent(eventId, eventType, aggregateId, userId, createdAt);
    }

    public void markPublished(Instant at) {
        this.status = "PUBLISHED";
        this.publishedAt = at;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }
}
