package com.loanflow.policy.domain.enums;

/**
 * Lifecycle status of a policy
 */
public enum PolicyStatus {
    DRAFT,      // Being created/edited
    ACTIVE,     // Currently enforced
    INACTIVE,   // Disabled but not deleted
    ARCHIVED    // Historical, replaced by newer version
}
