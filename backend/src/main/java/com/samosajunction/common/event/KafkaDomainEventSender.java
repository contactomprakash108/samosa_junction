package com.samosajunction.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaDomainEventSender implements DomainEventSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventSender.class);

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final String topic;

    public KafkaDomainEventSender(KafkaTemplate<String, DomainEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void send(DomainEvent event) {
        kafkaTemplate.send(topic, event.aggregateId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error(
                                "Failed to publish {} eventId={} after commit. Dual-write gap: DB committed, broker did not.",
                                event.type(),
                                event.eventId(),
                                error
                        );
                        return;
                    }
                    log.info(
                            "Published {} eventId={} partition={} offset={}",
                            event.type(),
                            event.eventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
