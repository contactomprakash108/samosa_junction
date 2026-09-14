package com.samosajunction.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "samosa.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingDomainEventSender implements DomainEventSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventSender.class);

    @Override
    public void send(DomainEvent event) {
        log.info(
                "Domain event {} eventId={} aggregateId={} userId={}",
                event.type(),
                event.eventId(),
                event.aggregateId(),
                event.userId()
        );
    }
}
