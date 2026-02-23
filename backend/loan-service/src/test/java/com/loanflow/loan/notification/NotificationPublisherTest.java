package com.loanflow.loan.notification;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.dto.notification.NotificationEventType;
import com.loanflow.loan.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationPublisher (US-031).
 */
@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationPublisher publisher;

    @Nested
    @DisplayName("Event publishing")
    class EventPublishing {

        @Test
        @DisplayName("Should publish event to RabbitMQ with correct exchange and routing key")
        void shouldPublishEvent() {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(NotificationEventType.APPLICATION_SUBMITTED)
                    .applicationId(UUID.randomUUID())
                    .applicationNumber("APP-2026-000001")
                    .recipientEmail("customer@example.com")
                    .timestamp(Instant.now())
                    .build();

            publisher.publish(event);

            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConfig.NOTIFICATION_EXCHANGE),
                    eq("loan.notification.application_submitted"),
                    eq(event)
            );
        }

        @Test
        @DisplayName("Should not throw exception when RabbitMQ publish fails (fire-and-forget)")
        void shouldNotThrowOnPublishFailure() {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(NotificationEventType.APPLICATION_APPROVED)
                    .applicationId(UUID.randomUUID())
                    .recipientEmail("customer@example.com")
                    .timestamp(Instant.now())
                    .build();

            doThrow(new RuntimeException("Connection refused"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }
    }
}
