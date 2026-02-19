package com.loanflow.loan.domain.enums;

public enum LoanType {
    HOME_LOAN("Home Loan", 8.5, 30),
    PERSONAL_LOAN("Personal Loan", 12.0, 7),
    VEHICLE_LOAN("Vehicle Loan", 9.5, 7),
    BUSINESS_LOAN("Business Loan", 14.0, 15),
    EDUCATION_LOAN("Education Loan", 8.0, 15),
    GOLD_LOAN("Gold Loan", 7.5, 3),
    LAP("Loan Against Property", 9.0, 15);

    private final String displayName;
    private final double baseInterestRate;
    private final int maxTenureYears;

    LoanType(String displayName, double baseInterestRate, int maxTenureYears) {
        this.displayName = displayName;
        this.baseInterestRate = baseInterestRate;
        this.maxTenureYears = maxTenureYears;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseInterestRate() {
        return baseInterestRate;
    }

    public int getMaxTenureYears() {
        return maxTenureYears;
    }

    public int getMaxTenureMonths() {
        return maxTenureYears * 12;
    }
}
