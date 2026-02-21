package com.loanflow.policy.domain.enums;

/**
 * Types of loans the policy engine supports
 */
public enum LoanType {
    PERSONAL_LOAN,
    HOME_LOAN,
    VEHICLE_LOAN,
    EDUCATION_LOAN,
    GOLD_LOAN,
    BUSINESS_LOAN,
    KCC,                // Kisan Credit Card
    LAP,                // Loan Against Property
    ALL                 // Applies to all loan types
}
