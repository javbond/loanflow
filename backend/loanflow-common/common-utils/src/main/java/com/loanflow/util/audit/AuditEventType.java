package com.loanflow.util.audit;

/**
 * Enumeration of all audit event types for the LoanFlow system (US-030).
 */
public final class AuditEventType {

    private AuditEventType() {
        // Utility class
    }

    // ==================== Loan Application Events ====================
    public static final String APPLICATION_CREATED = "APPLICATION_CREATED";
    public static final String APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    public static final String APPLICATION_UPDATED = "APPLICATION_UPDATED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";

    // ==================== Decision Events ====================
    public static final String DECISION_MADE = "DECISION_MADE";
    public static final String APPROVAL_GRANTED = "APPROVAL_GRANTED";
    public static final String APPLICATION_REJECTED = "APPLICATION_REJECTED";
    public static final String APPLICATION_REFERRED = "APPLICATION_REFERRED";

    // ==================== Document Events ====================
    public static final String DOCUMENT_UPLOADED = "DOCUMENT_UPLOADED";
    public static final String DOCUMENT_VERIFIED = "DOCUMENT_VERIFIED";
    public static final String DOCUMENT_REJECTED = "DOCUMENT_REJECTED";

    // ==================== KYC Events ====================
    public static final String KYC_INITIATED = "KYC_INITIATED";
    public static final String KYC_VERIFIED = "KYC_VERIFIED";

    // ==================== Workflow Events ====================
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_COMPLETED = "TASK_COMPLETED";
    public static final String CREDIT_CHECK_COMPLETED = "CREDIT_CHECK_COMPLETED";
    public static final String INCOME_VERIFIED = "INCOME_VERIFIED";

    // ==================== Customer Events ====================
    public static final String CUSTOMER_CREATED = "CUSTOMER_CREATED";
    public static final String CUSTOMER_UPDATED = "CUSTOMER_UPDATED";
}
