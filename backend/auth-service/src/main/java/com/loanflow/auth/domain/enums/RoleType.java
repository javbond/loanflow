package com.loanflow.auth.domain.enums;

/**
 * Role types for LoanFlow system
 */
public enum RoleType {
    ADMIN("Administrator with full access"),
    LOAN_OFFICER("Creates and manages loan applications"),
    UNDERWRITER("Reviews and decides on loan applications"),
    SENIOR_UNDERWRITER("Senior approval authority for higher limits"),
    CUSTOMER("End user/applicant");

    private final String description;

    RoleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
