package com.samosajunction.common.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentEventProcessorTest {

    @Mock
    private ProcessedDomainEventRepository processedDomainEventRepository;

    private IdempotentEventProcessor processor;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        processor = new IdempotentEventProcessor(processedDomainEventRepository);
        eventId = UUID.randomUUID();
    }

    @Test
    void firstClaimSucceeds() {
        when(processedDomainEventRepository.existsByEventIdAndConsumerName(eventId, "notification"))
                .thenReturn(false);

        assertThat(processor.claim(eventId, "notification")).isTrue();
        verify(processedDomainEventRepository).saveAndFlush(any(ProcessedDomainEvent.class));
    }

    @Test
    void secondClaimIsRejected() {
        when(processedDomainEventRepository.existsByEventIdAndConsumerName(eventId, "notification"))
                .thenReturn(true);

        assertThat(processor.claim(eventId, "notification")).isFalse();
        verify(processedDomainEventRepository, never()).saveAndFlush(any());
    }
}
