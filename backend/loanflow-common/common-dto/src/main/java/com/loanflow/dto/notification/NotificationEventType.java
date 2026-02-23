package com.loanflow.dto.notification;

/**
 * Constants for notification event types (US-031).
 */
public final class NotificationEventType {

    private NotificationEventType() {
        // Utility class
    }

    public static final String APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    public static final String APPLICATION_APPROVED = "APPLICATION_APPROVED";
    public static final String APPLICATION_REJECTED = "APPLICATION_REJECTED";
    public static final String APPLICATION_RETURNED = "APPLICATION_RETURNED";
    public static final String DOCUMENT_VERIFIED = "DOCUMENT_VERIFIED";
    public static final String KYC_COMPLETED = "KYC_COMPLETED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
}
