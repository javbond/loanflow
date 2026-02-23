package com.loanflow.notification.channel;

import com.loanflow.dto.notification.NotificationEvent;
import com.loanflow.dto.notification.NotificationEventType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Email notification channel using Thymeleaf templates (US-031).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${loanflow.notification.from-email:noreply@loanflow.com}")
    private String fromEmail;

    @Value("${loanflow.notification.from-name:LoanFlow}")
    private String fromName;

    /**
     * Send an email notification based on the event type.
     */
    public void send(NotificationEvent event) {
        try {
            String subject = getSubject(event);
            String body = renderTemplate(event);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(event.getRecipientEmail());
            helper.setSubject(subject);
            helper.setText(body, true); // HTML content

            mailSender.send(message);
            log.info("Email sent: type={}, to={}, subject={}",
                    event.getEventType(), event.getRecipientEmail(), subject);
        } catch (Exception e) {
            log.error("Failed to send email: type={}, to={}, error={}",
                    event.getEventType(), event.getRecipientEmail(), e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }

    String getSubject(NotificationEvent event) {
        String appNumber = event.getApplicationNumber() != null
                ? event.getApplicationNumber() : "your application";

        return switch (event.getEventType()) {
            case NotificationEventType.APPLICATION_SUBMITTED ->
                    "Application " + appNumber + " - Received Successfully";
            case NotificationEventType.APPLICATION_APPROVED ->
                    "Application " + appNumber + " - Approved!";
            case NotificationEventType.APPLICATION_REJECTED ->
                    "Application " + appNumber + " - Decision Update";
            case NotificationEventType.APPLICATION_RETURNED ->
                    "Application " + appNumber + " - Action Required";
            case NotificationEventType.DOCUMENT_VERIFIED ->
                    "Application " + appNumber + " - Document Verified";
            case NotificationEventType.KYC_COMPLETED ->
                    "Application " + appNumber + " - KYC Verification Complete";
            case NotificationEventType.TASK_ASSIGNED ->
                    "New Task Assigned - " + appNumber;
            default ->
                    "LoanFlow Update - " + appNumber;
        };
    }

    String renderTemplate(NotificationEvent event) {
        Context context = new Context();
        context.setVariable("event", event);
        context.setVariable("applicationNumber", event.getApplicationNumber());
        context.setVariable("recipientName", event.getRecipientName() != null
                ? event.getRecipientName() : "Valued Customer");
        context.setVariable("loanType", event.getLoanType());
        context.setVariable("status", event.getNewStatus());
        context.setVariable("reason", event.getReason());
        context.setVariable("approvedAmount", event.getApprovedAmount());
        context.setVariable("interestRate", event.getInterestRate());

        String templateName = getTemplateName(event.getEventType());

        try {
            return templateEngine.process("email/" + templateName, context);
        } catch (Exception e) {
            log.warn("Template '{}' not found, using default template", templateName);
            return templateEngine.process("email/default-notification", context);
        }
    }

    private String getTemplateName(String eventType) {
        return switch (eventType) {
            case NotificationEventType.APPLICATION_SUBMITTED -> "application-submitted";
            case NotificationEventType.APPLICATION_APPROVED -> "application-approved";
            case NotificationEventType.APPLICATION_REJECTED -> "application-rejected";
            case NotificationEventType.APPLICATION_RETURNED -> "application-returned";
            default -> "default-notification";
        };
    }
}
