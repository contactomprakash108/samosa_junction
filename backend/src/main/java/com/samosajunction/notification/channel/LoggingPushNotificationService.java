package com.samosajunction.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPushNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushNotificationService.class);

    @Override
    public String name() {
        return "PUSH";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("PUSH to {} :: {}", message.recipientKey(), message.body());
    }
}
