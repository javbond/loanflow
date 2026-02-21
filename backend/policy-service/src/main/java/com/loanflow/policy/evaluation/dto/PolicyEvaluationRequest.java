package com.loanflow.policy.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for policy evaluation.
 * Contains loan application data that will be used to build the evaluation context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationRequest {

    /**
     * Loan application ID (for audit trail)
     */
    @NotBlank(message = "Application ID is required")
    private String applicationId;

    /**
     * Loan type (must match LoanType enum)
     */
    @NotBlank(message = "Loan type is required")
    private String loanType;

    /**
     * Requested loan amount
     */
    @NotNull(message = "Requested amount is required")
    private BigDecimal requestedAmount;

    /**
     * Loan tenure in months
     */
    @NotNull(message = "Tenure is required")
    private Integer tenureMonths;

    /**
     * Purpose of the loan
     */
    private String purpose;

    /**
     * Branch code
     */
    private String branchCode;

    /**
     * Applicant CIBIL score
     */
    private Integer cibilScore;

    /**
     * Risk category (LOW, MEDIUM, HIGH)
     */
    private String riskCategory;

    /**
     * Applicant age
     */
    private Integer applicantAge;

    /**
     * Employment type (SALARIED, SELF_EMPLOYED, BUSINESS, PROFESSIONAL)
     */
    private String employmentType;

    /**
     * Monthly income
     */
    private BigDecimal monthlyIncome;

    /**
     * Years of work experience
     */
    private Integer yearsOfExperience;

    /**
     * Property estimated value (for secured loans)
     */
    private BigDecimal propertyValue;

    /**
     * Property type (APARTMENT, HOUSE, PLOT, etc.)
     */
    private String propertyType;

    /**
     * Additional custom fields for evaluation.
     * Allows extending evaluation context without changing the DTO structure.
     */
    private Map<String, String> additionalFields;

    /**
     * Build an EvaluationContext from this request
     */
    public EvaluationContext toEvaluationContext() {
        EvaluationContext context = new EvaluationContext();

        // Loan fields
        context.put("loan.type", loanType);
        context.put("loan.requestedAmount", requestedAmount);
        context.put("loan.tenureMonths", tenureMonths);
        if (purpose != null) context.put("loan.purpose", purpose);
        if (branchCode != null) context.put("loan.branchCode", branchCode);

        // Applicant fields
        if (cibilScore != null) context.put("applicant.cibilScore", cibilScore);
        if (riskCategory != null) context.put("applicant.riskCategory", riskCategory);
        if (applicantAge != null) context.put("applicant.age", applicantAge);

        // Employment fields
        if (employmentType != null) context.put("applicant.employmentType", employmentType);
        if (monthlyIncome != null) context.put("applicant.monthlyIncome", monthlyIncome);
        if (yearsOfExperience != null) context.put("applicant.yearsOfExperience", yearsOfExperience);

        // Property fields
        if (propertyValue != null) context.put("property.estimatedValue", propertyValue);
        if (propertyType != null) context.put("property.type", propertyType);

        // Additional custom fields
        if (additionalFields != null) {
            additionalFields.forEach(context::put);
        }

        return context;
    }
}
