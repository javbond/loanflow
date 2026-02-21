package com.loanflow.loan.decision.model;

/**
 * Employment type classifications used by Drools eligibility rules.
 */
public enum EmploymentType {
    SALARIED("Salaried"),
    SELF_EMPLOYED_PROFESSIONAL("Self-Employed Professional"),
    SELF_EMPLOYED_BUSINESS("Self-Employed Business"),
    RETIRED("Retired"),
    FARMER("Farmer"),
    STUDENT("Student");

    private final String displayName;

    EmploymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
