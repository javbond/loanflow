package com.loanflow.notification.service;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.notification.channel.EmailNotificationChannel;
import com.loanflow.notification.channel.SmsNotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dispatches notification events to the appropriate channels (US-031).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final EmailNotificationChannel emailChannel;
    private final SmsNotificationChannel smsChannel;

    @Value("${loanflow.notification.channels.email:true}")
    private boolean emailEnabled;

    @Value("${loanflow.notification.channels.sms:false}")
    private boolean smsEnabled;

    @Value("${loanflow.notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Dispatch a notification event to all enabled channels.
     */
    public void dispatch(NotificationEvent event) {
        if (!notificationsEnabled) {
            log.debug("Notifications are disabled, skipping event: {}", event.getEventType());
            return;
        }

        if (emailEnabled && event.getRecipientEmail() != null) {
            try {
                emailChannel.send(event);
            } catch (Exception e) {
                log.error("Email notification failed for {}: {}", event.getEventType(), e.getMessage());
            }
        }

        if (smsEnabled && event.getRecipientMobile() != null) {
            try {
                smsChannel.send(event);
            } catch (Exception e) {
                log.error("SMS notification failed for {}: {}", event.getEventType(), e.getMessage());
            }
        }
    }
}
