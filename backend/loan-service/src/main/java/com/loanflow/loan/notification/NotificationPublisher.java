package com.loanflow.loan.notification;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.loan.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes notification events to RabbitMQ (US-031).
 * Fire-and-forget pattern — notifications must not block business operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a notification event to RabbitMQ.
     *
     * @param event the notification event
     */
    public void publish(NotificationEvent event) {
        try {
            String routingKey = "loan.notification." + event.getEventType().toLowerCase();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    routingKey,
                    event
            );
            log.info("Published notification event: type={}, applicationId={}, recipient={}",
                    event.getEventType(), event.getApplicationId(), event.getRecipientEmail());
        } catch (Exception e) {
            // Notification publishing must never fail the business operation
            log.error("Failed to publish notification event: type={}, error={}",
                    event.getEventType(), e.getMessage());
        }
    }
}
