package com.loanflow.document.domain.enums;

/**
 * Categories for organizing documents
 */
public enum DocumentCategory {
    KYC("KYC Documents", true),
    INCOME("Income Proof", true),
    FINANCIAL("Financial Documents", true),
    PROPERTY("Property Documents", false),
    VEHICLE("Vehicle Documents", false),
    BUSINESS("Business Documents", false),
    OTHER("Other Documents", false);

    private final String displayName;
    private final boolean mandatoryForAllLoans;

    DocumentCategory(String displayName, boolean mandatoryForAllLoans) {
        this.displayName = displayName;
        this.mandatoryForAllLoans = mandatoryForAllLoans;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isMandatoryForAllLoans() {
        return mandatoryForAllLoans;
    }
}
