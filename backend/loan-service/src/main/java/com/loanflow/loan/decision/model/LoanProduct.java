package com.loanflow.loan.decision.model;

/**
 * Loan product codes used by Drools DRL rules.
 * Maps to the productCode field in LoanApplicationFact.
 */
public enum LoanProduct {
    PL("Personal Loan"),
    HL("Home Loan"),
    VL("Vehicle Loan"),
    GL("Gold Loan"),
    KCC("Kisan Credit Card"),
    EL("Education Loan"),
    BL("Business Loan"),
    LAP("Loan Against Property");

    private final String displayName;

    LoanProduct(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
