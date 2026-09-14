package com.samosajunction.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "samosa.outbox.poller-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxDomainEventPublisher publisher;

    public OutboxPoller(OutboxDomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${samosa.outbox.poll-ms:5000}")
    public void poll() {
        int sent = publisher.publishPending();
        if (sent > 0) {
            log.info("Outbox published {} pending event(s)", sent);
        }
    }
}
