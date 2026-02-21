package com.loanflow.loan.decision.model;

/**
 * Employer category classifications used by Drools pricing rules.
 */
public enum EmployerCategory {
    GOVERNMENT("Government"),
    PSU("Public Sector Undertaking"),
    MNC("Multinational Corporation"),
    LISTED_COMPANY("Listed Company"),
    PRIVATE("Private Company"),
    STARTUP("Startup"),
    SELF_EMPLOYED("Self-Employed"),
    OTHER("Other");

    private final String displayName;

    EmployerCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
