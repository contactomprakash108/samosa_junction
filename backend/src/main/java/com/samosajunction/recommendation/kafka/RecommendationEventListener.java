package com.samosajunction.recommendation.kafka;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.IdempotentEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "samosa.kafka", name = "enabled", havingValue = "true")
public class RecommendationEventListener {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEventListener.class);

    private final IdempotentEventProcessor idempotentEventProcessor;

    public RecommendationEventListener(IdempotentEventProcessor idempotentEventProcessor) {
        this.idempotentEventProcessor = idempotentEventProcessor;
    }

    @KafkaListener(
            topics = "${samosa.kafka.topic}",
            groupId = "samosa-recommendation",
            containerFactory = "domainEventKafkaListenerContainerFactory"
    )
    public void onEvent(DomainEvent event) {
        if (!idempotentEventProcessor.claim(event.eventId(), "recommendation")) {
            return;
        }
        log.info("Recommendation observed {} userId={} (profile still derived from paid orders on read)",
                event.type(), event.userId());
    }
}
