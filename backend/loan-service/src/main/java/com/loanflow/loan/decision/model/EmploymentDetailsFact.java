package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drools fact representing employment details of an applicant.
 * Used by eligibility (income checks) and pricing (employer discount) rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentDetailsFact {

    private String id;
    private String applicantId;

    private EmploymentType employmentType;
    private EmployerCategory employerCategory;

    private double netMonthlyIncome;
    private double totalExperienceYears;
    private double yearsInCurrentJob;
}
