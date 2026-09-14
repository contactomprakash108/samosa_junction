package com.samosajunction.notification.kafka;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.IdempotentEventProcessor;
import com.samosajunction.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "samosa.kafka", name = "enabled", havingValue = "true")
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    static final String CONSUMER = "notification";

    private final IdempotentEventProcessor idempotentEventProcessor;
    private final NotificationService notificationService;

    public NotificationEventListener(
            IdempotentEventProcessor idempotentEventProcessor,
            NotificationService notificationService
    ) {
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${samosa.kafka.topic}",
            groupId = "samosa-notification",
            containerFactory = "domainEventKafkaListenerContainerFactory"
    )
    public void onEvent(DomainEvent event) {
        if (!idempotentEventProcessor.claim(event.eventId(), CONSUMER)) {
            log.info("Duplicate {} skipped for notification eventId={}", event.type(), event.eventId());
            return;
        }
        notificationService.notify(event);
    }
}
