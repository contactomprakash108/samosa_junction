package com.samosajunction.notification.channel;

public interface NotificationChannel {

    String name();

    void send(NotificationMessage message);
}
