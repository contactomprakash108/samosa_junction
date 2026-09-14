package com.samosajunction.notification.channel;

import com.samosajunction.common.event.DomainEvent;

public record NotificationMessage(DomainEvent event, String recipientKey, String body) {
}
