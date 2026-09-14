package com.samosajunction.notification.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.notification.channel.NotificationChannel;
import com.samosajunction.notification.channel.NotificationMessage;
import com.samosajunction.notification.entity.NotificationLog;
import com.samosajunction.notification.repository.NotificationLogRepository;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Set<DomainEventType> NOTIFIABLE = Set.of(
            DomainEventType.ORDER_CONFIRMED,
            DomainEventType.ORDER_PREPARING,
            DomainEventType.ORDER_READY,
            DomainEventType.ORDER_OUT_FOR_DELIVERY,
            DomainEventType.ORDER_DELIVERED,
            DomainEventType.ORDER_CANCELLED,
            DomainEventType.PAYMENT_FAILED,
            DomainEventType.COMPLAINT_CREATED,
            DomainEventType.COMPLAINT_UPDATED
    );

    private final List<NotificationChannel> channels;
    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;

    public NotificationService(
            List<NotificationChannel> channels,
            NotificationLogRepository notificationLogRepository,
            UserRepository userRepository
    ) {
        this.channels = channels;
        this.notificationLogRepository = notificationLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notify(DomainEvent event) {
        if (!NOTIFIABLE.contains(event.type())) {
            return;
        }
        String recipient = userRepository.findById(event.userId())
                .map(User::getEmail)
                .orElse("user:" + event.userId());
        String body = switch (event.type()) {
            case ORDER_CONFIRMED -> "Your Samosa Junction order is confirmed.";
            case ORDER_PREPARING -> "Your Samosa Junction order is being prepared.";
            case ORDER_READY -> "Your Samosa Junction order is ready.";
            case ORDER_OUT_FOR_DELIVERY -> "Your Samosa Junction order is out for delivery.";
            case ORDER_DELIVERED -> "Your Samosa Junction order has been delivered. Enjoy.";
            case ORDER_CANCELLED -> "Your Samosa Junction order was cancelled.";
            case PAYMENT_FAILED -> "Payment for your Samosa Junction order failed. You can retry.";
            case COMPLAINT_CREATED -> "We received your complaint. Our team will review it.";
            case COMPLAINT_UPDATED -> "Your complaint status was updated.";
            default -> event.type().name();
        };
        NotificationMessage message = new NotificationMessage(event, recipient, body);
        for (NotificationChannel channel : channels) {
            channel.send(message);
            notificationLogRepository.save(new NotificationLog(
                    event.eventId(),
                    event.type().name(),
                    channel.name(),
                    recipient
            ));
        }
        log.debug("Notified {} channels for {}", channels.size(), event.type());
    }

    @Transactional
    public void notifyPasswordReset(String email, String resetUrl) {
        UUID eventId = UUID.randomUUID();
        var event = DomainEvent.of(DomainEventType.PASSWORD_RESET, eventId, eventId, eventId.toString());
        String body = "Reset your Samosa Junction password: " + resetUrl;
        NotificationMessage message = new NotificationMessage(event, email, body);
        for (NotificationChannel channel : channels) {
            if (!"EMAIL".equals(channel.name())) {
                continue;
            }
            channel.send(message);
            notificationLogRepository.save(new NotificationLog(eventId, "PASSWORD_RESET", channel.name(), email));
        }
    }
}
