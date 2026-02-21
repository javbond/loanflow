package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drools fact representing a loan applicant.
 * Used by eligibility and pricing DRL rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantFact {

    private String id;
    private String applicationId;

    /** PRIMARY or CO_APPLICANT */
    private String applicantType;

    private int age;
    private String gender;  // MALE, FEMALE, OTHER

    // KYC
    private String pan;
    private boolean panVerified;
    private boolean politicallyExposed;

    // Financial
    private double existingEmi;
    private boolean hasSalaryAccount;

    // Customer relationship
    private String customerSegment;  // PREMIUM, REGULAR, etc.
    private boolean existingCustomer;
    private int existingLoanDpd;  // Days Past Due on existing loans
}
