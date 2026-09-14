package com.samosajunction.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotificationService.class);

    @Override
    public String name() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("EMAIL to {} :: {}", message.recipientKey(), message.body());
    }
}
