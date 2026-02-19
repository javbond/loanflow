package com.loanflow.customer.domain.enums;

public enum CustomerStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    BLOCKED("Blocked"),
    PENDING_VERIFICATION("Pending Verification");

    private final String displayName;

    CustomerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransact() {
        return this == ACTIVE;
    }
}
