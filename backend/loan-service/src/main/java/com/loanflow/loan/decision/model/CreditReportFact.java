package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drools fact representing a credit bureau report (CIBIL/Experian).
 * Used by eligibility (score checks) and pricing (risk-based pricing) rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditReportFact {

    private String id;
    private String applicantId;

    private int creditScore;           // CIBIL score: 300-900
    private int dpd90PlusCount;        // Number of accounts with 90+ DPD
    private int writtenOffAccounts;    // Number of written-off accounts
    private int enquiryCount30Days;    // Credit enquiries in last 30 days
}
