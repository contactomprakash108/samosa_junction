package com.samosajunction.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsNotificationService.class);

    @Override
    public String name() {
        return "SMS";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("SMS to {} :: {}", message.recipientKey(), message.body());
    }
}
