package com.loanflow.notification.channel;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.dto.notification.NotificationEventType;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailNotificationChannel (US-031).
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailNotificationChannel emailChannel;

    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailChannel, "fromEmail", "noreply@loanflow.com");
        ReflectionTestUtils.setField(emailChannel, "fromName", "LoanFlow");

        testEvent = NotificationEvent.builder()
                .eventType(NotificationEventType.APPLICATION_SUBMITTED)
                .applicationId(UUID.randomUUID())
                .applicationNumber("APP-2026-000001")
                .recipientEmail("customer@example.com")
                .recipientName("John Doe")
                .loanType("HOME_LOAN")
                .timestamp(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Subject generation")
    class SubjectGeneration {

        @Test
        @DisplayName("Should generate correct subject for APPLICATION_SUBMITTED")
        void subjectForSubmitted() {
            testEvent.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
            String subject = emailChannel.getSubject(testEvent);
            assertThat(subject).isEqualTo("Application APP-2026-000001 - Received Successfully");
        }

        @Test
        @DisplayName("Should generate correct subject for APPLICATION_APPROVED")
        void subjectForApproved() {
            testEvent.setEventType(NotificationEventType.APPLICATION_APPROVED);
            String subject = emailChannel.getSubject(testEvent);
            assertThat(subject).isEqualTo("Application APP-2026-000001 - Approved!");
        }

        @Test
        @DisplayName("Should generate correct subject for APPLICATION_REJECTED")
        void subjectForRejected() {
            testEvent.setEventType(NotificationEventType.APPLICATION_REJECTED);
            String subject = emailChannel.getSubject(testEvent);
            assertThat(subject).isEqualTo("Application APP-2026-000001 - Decision Update");
        }

        @Test
        @DisplayName("Should generate correct subject for APPLICATION_RETURNED")
        void subjectForReturned() {
            testEvent.setEventType(NotificationEventType.APPLICATION_RETURNED);
            String subject = emailChannel.getSubject(testEvent);
            assertThat(subject).isEqualTo("Application APP-2026-000001 - Action Required");
        }

        @Test
        @DisplayName("Should use fallback when application number is null")
        void subjectWithNullAppNumber() {
            testEvent.setApplicationNumber(null);
            testEvent.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
            String subject = emailChannel.getSubject(testEvent);
            assertThat(subject).isEqualTo("Application your application - Received Successfully");
        }

        @Test
        @DisplayName("Should generate default subject for unknown event type")
        void subjectForUnknownType() {
            testEvent.setEventType("UNKNOWN_EVENT");
            String subject = emailChannel.getSubject(testEvent);
            assertThat(subject).startsWith("LoanFlow Update");
        }
    }

    @Nested
    @DisplayName("Template rendering")
    class TemplateRendering {

        @Test
        @DisplayName("Should render correct template for submitted event")
        void renderSubmittedTemplate() {
            when(templateEngine.process(eq("email/application-submitted"), any(Context.class)))
                    .thenReturn("<html>Submitted</html>");

            String body = emailChannel.renderTemplate(testEvent);

            assertThat(body).isEqualTo("<html>Submitted</html>");
            verify(templateEngine).process(eq("email/application-submitted"), any(Context.class));
        }

        @Test
        @DisplayName("Should fall back to default template when specific template not found")
        void fallbackToDefaultTemplate() {
            when(templateEngine.process(eq("email/application-submitted"), any(Context.class)))
                    .thenThrow(new RuntimeException("Template not found"));
            when(templateEngine.process(eq("email/default-notification"), any(Context.class)))
                    .thenReturn("<html>Default</html>");

            String body = emailChannel.renderTemplate(testEvent);

            assertThat(body).isEqualTo("<html>Default</html>");
            verify(templateEngine).process(eq("email/default-notification"), any(Context.class));
        }
    }

    @Nested
    @DisplayName("Email sending")
    class EmailSending {

        @Test
        @DisplayName("Should send email via JavaMailSender")
        void shouldSendEmail() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenReturn("<html>Test body</html>");

            emailChannel.send(testEvent);

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should throw RuntimeException when mail sending fails")
        void shouldThrowOnMailError() throws Exception {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenReturn("<html>Body</html>");
            doThrow(new RuntimeException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailChannel.send(testEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email sending failed");
        }
    }
}
