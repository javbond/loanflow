package com.loanflow.notification.consumer;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that receives notification events (US-031).
 * Delegates to NotificationDispatcher for channel-specific handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationDispatcher dispatcher;

    @RabbitListener(queues = "notifications.queue")
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: type={}, applicationId={}, recipient={}",
                event.getEventType(), event.getApplicationId(), event.getRecipientEmail());

        try {
            dispatcher.dispatch(event);
        } catch (Exception e) {
            log.error("Failed to process notification event: type={}, error={}",
                    event.getEventType(), e.getMessage(), e);
            // Message will go to DLQ after retry exhaustion
            throw e;
        }
    }
}
