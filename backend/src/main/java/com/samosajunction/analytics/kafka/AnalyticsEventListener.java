package com.samosajunction.analytics.kafka;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.IdempotentEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "samosa.kafka", name = "enabled", havingValue = "true")
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);

    private final IdempotentEventProcessor idempotentEventProcessor;

    public AnalyticsEventListener(IdempotentEventProcessor idempotentEventProcessor) {
        this.idempotentEventProcessor = idempotentEventProcessor;
    }

    @KafkaListener(
            topics = "${samosa.kafka.topic}",
            groupId = "samosa-analytics",
            containerFactory = "domainEventKafkaListenerContainerFactory"
    )
    public void onEvent(DomainEvent event) {
        if (!idempotentEventProcessor.claim(event.eventId(), "analytics")) {
            return;
        }
        log.info("Analytics ingested {} aggregateId={}", event.type(), event.aggregateId());
    }
}
