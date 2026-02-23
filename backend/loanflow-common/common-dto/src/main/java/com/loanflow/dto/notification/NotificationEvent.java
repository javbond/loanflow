package com.loanflow.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event message published to RabbitMQ for notification processing (US-031).
 * Consumed by notification-service to send email/SMS notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent implements Serializable {

    /** Event type (e.g., APPLICATION_SUBMITTED, APPROVAL_GRANTED) */
    private String eventType;

    /** Loan application ID */
    private UUID applicationId;

    /** Application number for display */
    private String applicationNumber;

    /** Customer ID */
    private UUID customerId;

    /** Recipient email address */
    private String recipientEmail;

    /** Recipient mobile number (for SMS) */
    private String recipientMobile;

    /** Recipient name for personalization */
    private String recipientName;

    /** Old status (for status change events) */
    private String oldStatus;

    /** New status */
    private String newStatus;

    /** Reason (for rejection/return events) */
    private String reason;

    /** Approved amount (for approval events) */
    private BigDecimal approvedAmount;

    /** Interest rate (for approval events) */
    private BigDecimal interestRate;

    /** EMI amount (for approval events) */
    private BigDecimal emiAmount;

    /** Assigned officer name (for task assignment events) */
    private String assignedOfficerName;

    /** Loan type */
    private String loanType;

    /** Service that published the event */
    private String sourceName;

    /** Additional data */
    private Map<String, Object> metadata;

    /** Event timestamp */
    private Instant timestamp;
}
