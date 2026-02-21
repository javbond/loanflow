package com.loanflow.policy.domain.enums;

/**
 * Types of actions a policy can trigger
 */
public enum ActionType {
    APPROVE,           // Auto-approve the application
    REJECT,            // Auto-reject the application
    REFER,             // Refer to higher authority
    SET_INTEREST_RATE, // Set interest rate
    SET_PROCESSING_FEE,// Set processing fee percentage
    SET_MAX_AMOUNT,    // Set maximum loan amount
    SET_MAX_TENURE,    // Set maximum loan tenure
    REQUIRE_DOCUMENT,  // Require additional document
    ASSIGN_TO_ROLE,    // Assign task to specific role
    NOTIFY,            // Send notification
    FLAG_RISK          // Flag for risk review
}
