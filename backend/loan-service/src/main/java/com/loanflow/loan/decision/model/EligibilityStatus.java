package com.loanflow.loan.decision.model;

/**
 * Eligibility determination status set by Drools eligibility rules.
 */
public enum EligibilityStatus {
    ELIGIBLE("Eligible"),
    CONDITIONALLY_ELIGIBLE("Conditionally Eligible"),
    NOT_ELIGIBLE("Not Eligible"),
    REJECTED("Rejected"),
    REFER("Referred to Senior"),
    PENDING_KYC("Pending KYC Verification");

    private final String displayName;

    EligibilityStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
