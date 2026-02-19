package com.loanflow.customer.domain.enums;

public enum KycStatus {
    PENDING("Pending"),
    PARTIAL("Partially Verified"),
    VERIFIED("Fully Verified"),
    EXPIRED("Expired"),
    REJECTED("Rejected");

    private final String displayName;

    KycStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isComplete() {
        return this == VERIFIED;
    }
}
