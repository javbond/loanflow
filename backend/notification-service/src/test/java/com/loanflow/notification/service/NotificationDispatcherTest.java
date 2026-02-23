package com.loanflow.notification.service;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.dto.notification.NotificationEventType;
import com.loanflow.notification.channel.EmailNotificationChannel;
import com.loanflow.notification.channel.SmsNotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationDispatcher (US-031).
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailNotificationChannel emailChannel;

    @Mock
    private SmsNotificationChannel smsChannel;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    private NotificationEvent baseEvent;

    @BeforeEach
    void setUp() {
        // Default: all channels enabled
        ReflectionTestUtils.setField(dispatcher, "notificationsEnabled", true);
        ReflectionTestUtils.setField(dispatcher, "emailEnabled", true);
        ReflectionTestUtils.setField(dispatcher, "smsEnabled", false);

        baseEvent = NotificationEvent.builder()
                .eventType(NotificationEventType.APPLICATION_SUBMITTED)
                .applicationId(UUID.randomUUID())
                .applicationNumber("APP-2026-000001")
                .recipientEmail("customer@example.com")
                .recipientMobile("+919876543210")
                .recipientName("Test Customer")
                .timestamp(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Dispatch with email enabled")
    class EmailEnabled {

        @Test
        @DisplayName("Should send email when email channel is enabled and recipient has email")
        void shouldSendEmailWhenEnabled() {
            dispatcher.dispatch(baseEvent);

            verify(emailChannel, times(1)).send(baseEvent);
            verify(smsChannel, never()).send(any());
        }

        @Test
        @DisplayName("Should NOT send email when recipient has no email address")
        void shouldNotSendEmailWhenNoRecipientEmail() {
            baseEvent.setRecipientEmail(null);

            dispatcher.dispatch(baseEvent);

            verify(emailChannel, never()).send(any());
        }
    }

    @Nested
    @DisplayName("Dispatch with SMS enabled")
    class SmsEnabled {

        @Test
        @DisplayName("Should send SMS when SMS channel is enabled and recipient has mobile")
        void shouldSendSmsWhenEnabled() {
            ReflectionTestUtils.setField(dispatcher, "smsEnabled", true);

            dispatcher.dispatch(baseEvent);

            verify(smsChannel, times(1)).send(baseEvent);
            verify(emailChannel, times(1)).send(baseEvent);
        }

        @Test
        @DisplayName("Should NOT send SMS when recipient has no mobile number")
        void shouldNotSendSmsWhenNoMobile() {
            ReflectionTestUtils.setField(dispatcher, "smsEnabled", true);
            baseEvent.setRecipientMobile(null);

            dispatcher.dispatch(baseEvent);

            verify(smsChannel, never()).send(any());
        }
    }

    @Nested
    @DisplayName("Dispatch when notifications disabled")
    class NotificationsDisabled {

        @Test
        @DisplayName("Should skip all channels when notifications are globally disabled")
        void shouldSkipAllWhenDisabled() {
            ReflectionTestUtils.setField(dispatcher, "notificationsEnabled", false);

            dispatcher.dispatch(baseEvent);

            verify(emailChannel, never()).send(any());
            verify(smsChannel, never()).send(any());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should not propagate email channel exceptions to SMS channel")
        void shouldNotPropagateEmailErrors() {
            ReflectionTestUtils.setField(dispatcher, "smsEnabled", true);
            doThrow(new RuntimeException("SMTP error")).when(emailChannel).send(any());

            dispatcher.dispatch(baseEvent);

            // SMS should still be called despite email failure
            verify(smsChannel, times(1)).send(baseEvent);
        }
    }
}
