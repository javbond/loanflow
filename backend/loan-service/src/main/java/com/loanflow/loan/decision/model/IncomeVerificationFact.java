package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drools fact for income verification data.
 * Inserted into KieSession when income verification is available.
 * Used by eligibility and pricing rules for DTI checks, income mismatch detection,
 * GST turnover verification, and cheque bounce assessment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeVerificationFact {

    /** Application ID — links to LoanApplicationFact.id */
    private String applicationId;

    /** Whether income was verified successfully */
    private boolean incomeVerified;

    /** Verified monthly income in INR */
    private double verifiedMonthlyIncome;

    /** Debt-to-Income ratio (0.0 to 1.0) */
    private double dtiRatio;

    /** Income consistency score (0-100): declared vs verified income match */
    private int incomeConsistencyScore;

    /** Annual income from ITR */
    private double annualItrIncome;

    /** Annual GST turnover (0 if not applicable) */
    private double annualGstTurnover;

    /** Number of GST filings in last 12 months (0 if no GSTIN) */
    private int gstFilingCount12Months;

    /** GST compliance rating: EXCELLENT, GOOD, FAIR, POOR (null if no GSTIN) */
    private String gstComplianceRating;

    /** Average monthly bank balance over 6 months */
    private double avgMonthlyBankBalance;

    /** Average monthly salary/business credits in bank */
    private double avgMonthlySalaryCredits;

    /** Cheque/ECS bounce count in last 6 months */
    private int chequeBounceCount;
}
