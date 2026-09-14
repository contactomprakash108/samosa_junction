package com.samosajunction.notification.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.notification.channel.NotificationChannel;
import com.samosajunction.notification.entity.NotificationLog;
import com.samosajunction.notification.repository.NotificationLogRepository;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationChannel email;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(email.name()).thenReturn("EMAIL");
        notificationService = new NotificationService(List.of(email), notificationLogRepository, userRepository);
    }

    @Test
    void notifiesConfirmedOrders() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("ada@samosa.test");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        DomainEvent event = DomainEvent.of(DomainEventType.ORDER_CONFIRMED, UUID.randomUUID(), userId);

        notificationService.notify(event);

        verify(email).send(any());
        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    void notifyPasswordResetUsesEmailChannel() {
        notificationService.notifyPasswordReset("ada@samosa.test", "http://localhost:5173/reset-password?token=abc");

        verify(email).send(any());
        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    void skipsWalletDebited() {
        DomainEvent event = DomainEvent.walletDebited(UUID.randomUUID(), "order-debit:1");

        notificationService.notify(event);

        verify(email, never()).send(any());
        verify(notificationLogRepository, never()).save(any());
    }
}
