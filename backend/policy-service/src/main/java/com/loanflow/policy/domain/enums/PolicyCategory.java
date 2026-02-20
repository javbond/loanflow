package com.loanflow.policy.domain.enums;

/**
 * Categories of loan policies
 */
public enum PolicyCategory {
    ELIGIBILITY,       // Determines if applicant qualifies
    PRICING,           // Interest rate, processing fee rules
    CREDIT_LIMIT,      // Maximum loan amount rules
    DOCUMENT_REQUIREMENT, // Required documents per loan type
    WORKFLOW,          // Approval routing rules
    RISK_SCORING       // Risk assessment parameters
}
