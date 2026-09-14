package com.samosajunction.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 16)
    private String channel;

    @Column(name = "recipient_key", nullable = false, length = 128)
    private String recipientKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(UUID eventId, String eventType, String channel, String recipientKey) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.channel = channel;
        this.recipientKey = recipientKey;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }
}
