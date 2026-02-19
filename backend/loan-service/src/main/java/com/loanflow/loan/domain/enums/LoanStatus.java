package com.loanflow.loan.domain.enums;

import java.util.Set;

public enum LoanStatus {
    DRAFT("Draft", Set.of("SUBMITTED", "CANCELLED")),
    SUBMITTED("Submitted", Set.of("DOCUMENT_VERIFICATION", "RETURNED", "CANCELLED")),
    DOCUMENT_VERIFICATION("Document Verification", Set.of("CREDIT_CHECK", "RETURNED", "REJECTED")),
    CREDIT_CHECK("Credit Check", Set.of("UNDERWRITING", "REJECTED")),
    UNDERWRITING("Underwriting", Set.of("APPROVED", "CONDITIONALLY_APPROVED", "REJECTED", "REFERRED")),
    CONDITIONALLY_APPROVED("Conditionally Approved", Set.of("APPROVED", "REJECTED", "CANCELLED")),
    REFERRED("Referred to Senior", Set.of("APPROVED", "REJECTED")),
    APPROVED("Approved", Set.of("DISBURSEMENT_PENDING", "CANCELLED")),
    DISBURSEMENT_PENDING("Disbursement Pending", Set.of("DISBURSED", "CANCELLED")),
    DISBURSED("Disbursed", Set.of("CLOSED", "NPA")),
    RETURNED("Returned for Correction", Set.of("SUBMITTED", "CANCELLED")),
    REJECTED("Rejected", Set.of()),
    CANCELLED("Cancelled", Set.of()),
    CLOSED("Closed", Set.of()),
    NPA("Non-Performing Asset", Set.of("CLOSED"));

    private final String displayName;
    private final Set<String> allowedTransitions;

    LoanStatus(String displayName, Set<String> allowedTransitions) {
        this.displayName = displayName;
        this.allowedTransitions = allowedTransitions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getAllowedTransitions() {
        return allowedTransitions;
    }

    public boolean canTransitionTo(LoanStatus targetStatus) {
        return allowedTransitions.contains(targetStatus.name());
    }

    public boolean isTerminal() {
        return allowedTransitions.isEmpty() || this == CLOSED || this == REJECTED || this == CANCELLED;
    }

    public boolean isActive() {
        return !isTerminal() && this != NPA;
    }
}
