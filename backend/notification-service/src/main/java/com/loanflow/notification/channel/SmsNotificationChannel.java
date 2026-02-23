package com.loanflow.notification.channel;

import com.loanflow.dto.notification.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS notification channel — NoOp stub for dev/test (US-031).
 * In production, this would integrate with an SMS gateway (Twilio, MSG91, etc.).
 */
@Component
@Slf4j
public class SmsNotificationChannel {

    /**
     * Send an SMS notification. Currently a no-op stub.
     *
     * @param event the notification event
     */
    public void send(NotificationEvent event) {
        log.info("[SMS STUB] Would send SMS to {}: type={}, app={}",
                event.getRecipientMobile(),
                event.getEventType(),
                event.getApplicationNumber());

        // TODO: Integrate with SMS provider (Twilio, MSG91, etc.) in production
        // smsProvider.send(event.getRecipientMobile(), buildMessage(event));
    }
}
