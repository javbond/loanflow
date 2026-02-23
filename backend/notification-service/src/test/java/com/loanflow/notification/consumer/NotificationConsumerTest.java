package com.loanflow.notification.consumer;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.dto.notification.NotificationEventType;
import com.loanflow.notification.service.NotificationDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationConsumer (US-031).
 */
@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private NotificationConsumer consumer;

    @Nested
    @DisplayName("Message handling")
    class MessageHandling {

        @Test
        @DisplayName("Should delegate event to dispatcher")
        void shouldDelegateToDispatcher() {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(NotificationEventType.APPLICATION_SUBMITTED)
                    .applicationId(UUID.randomUUID())
                    .applicationNumber("APP-2026-000001")
                    .recipientEmail("customer@example.com")
                    .timestamp(Instant.now())
                    .build();

            consumer.handleNotificationEvent(event);

            verify(dispatcher, times(1)).dispatch(event);
        }

        @Test
        @DisplayName("Should re-throw exception for DLQ handling when dispatch fails")
        void shouldRethrowExceptionOnFailure() {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(NotificationEventType.APPLICATION_APPROVED)
                    .applicationId(UUID.randomUUID())
                    .recipientEmail("customer@example.com")
                    .timestamp(Instant.now())
                    .build();

            doThrow(new RuntimeException("Processing failed")).when(dispatcher).dispatch(any());

            assertThatThrownBy(() -> consumer.handleNotificationEvent(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Processing failed");
        }
    }
}
