package com.loanflow.policy.config;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Seeds pre-built policy templates into MongoDB on application startup.
 *
 * Creates 3 DRAFT eligibility templates:
 * - Personal Loan (4 rules: salaried approval, self-employed approval, low CIBIL rejection, borderline referral)
 * - Home Loan (4 rules: standard approval, high-value referral, low CIBIL rejection, income rejection)
 * - KCC / Kisan Credit Card (3 rules: standard approval, large farmer enhanced, no land rejection)
 *
 * Idempotent: skips templates that already exist (by name).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyTemplateInitializer implements ApplicationRunner {

    private final PolicyRepository policyRepository;

    private static final String CREATED_BY = "system";

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking for policy templates to initialize...");
        int created = 0;

        created += createIfNotExists(buildPersonalLoanEligibilityTemplate());
        created += createIfNotExists(buildHomeLoanEligibilityTemplate());
        created += createIfNotExists(buildKccEligibilityTemplate());

        if (created > 0) {
            log.info("Policy template initialization complete. Created {} new template(s).", created);
        } else {
            log.info("All policy templates already exist. No new templates created.");
        }
    }

    /**
     * Create a template if it doesn't already exist.
     * @return 1 if created, 0 if skipped
     */
    private int createIfNotExists(Policy template) {
        if (policyRepository.existsByNameIgnoreCase(template.getName())) {
            log.debug("Template already exists: '{}', skipping.", template.getName());
            return 0;
        }

        template.prePersist();
        template.setCreatedBy(CREATED_BY);
        template.setModifiedBy(CREATED_BY);

        Policy saved = policyRepository.save(template);
        log.info("Created policy template: '{}' (code: {}, loanType: {}, rules: {})",
                saved.getName(), saved.getPolicyCode(), saved.getLoanType(), saved.getRuleCount());
        return 1;
    }

    // ==================== Template Builders ====================
    // Package-private for testability

    /**
     * Personal Loan Eligibility Template
     *
     * Rules:
     * 1. Low CIBIL Rejection (priority 5): cibilScore < 500 → REJECT
     * 2. Salaried Approval (priority 10): salaried/professional + cibilScore >= 650 + age 21-60 + income >= 25K → APPROVE
     * 3. Borderline Referral (priority 15): cibilScore 500-649 → REFER to senior underwriter
     * 4. Self-Employed Approval (priority 20): self-employed/business + cibilScore >= 700 + age 25-55 + income >= 40K → APPROVE
     */
    Policy buildPersonalLoanEligibilityTemplate() {
        List<PolicyRule> rules = new ArrayList<>();

        // Rule 1: Low CIBIL Rejection (highest priority — evaluated first)
        rules.add(PolicyRule.builder()
                .name("Low CIBIL Rejection")
                .description("Reject applicants with CIBIL score below 500")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.LESS_THAN)
                                .value("500")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REJECT)
                                .description("CIBIL score below minimum threshold of 500")
                                .build()
                ))
                .priority(5)
                .enabled(true)
                .build());

        // Rule 2: Salaried Applicant Approval
        rules.add(PolicyRule.builder()
                .name("Salaried Applicant Approval")
                .description("Approve salaried/professional applicants meeting eligibility criteria")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.employmentType")
                                .operator(ConditionOperator.IN)
                                .values(List.of("SALARIED", "PROFESSIONAL"))
                                .build(),
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("650")
                                .build(),
                        Condition.builder()
                                .field("applicant.age")
                                .operator(ConditionOperator.BETWEEN)
                                .minValue("21")
                                .maxValue("60")
                                .build(),
                        Condition.builder()
                                .field("applicant.monthlyIncome")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("25000")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.APPROVE)
                                .description("Eligible for personal loan — salaried applicant")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_MAX_AMOUNT)
                                .parameters(Map.of("amount", "2000000"))
                                .description("Maximum loan amount: INR 20 lakhs")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_INTEREST_RATE)
                                .parameters(Map.of("rate", "12.5", "type", "FIXED"))
                                .description("Standard interest rate for salaried applicants")
                                .build()
                ))
                .priority(10)
                .enabled(true)
                .build());

        // Rule 3: Borderline CIBIL Referral
        rules.add(PolicyRule.builder()
                .name("Borderline CIBIL Referral")
                .description("Refer applicants with borderline CIBIL (500-649) to senior underwriter")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.BETWEEN)
                                .minValue("500")
                                .maxValue("649")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REFER)
                                .description("Borderline CIBIL score — requires senior underwriter review")
                                .build(),
                        Action.builder()
                                .type(ActionType.ASSIGN_TO_ROLE)
                                .parameters(Map.of("role", "SENIOR_UNDERWRITER"))
                                .description("Assign to senior underwriter for manual review")
                                .build(),
                        Action.builder()
                                .type(ActionType.FLAG_RISK)
                                .parameters(Map.of("reason", "Borderline CIBIL score"))
                                .description("Flag for risk review")
                                .build()
                ))
                .priority(15)
                .enabled(true)
                .build());

        // Rule 4: Self-Employed Approval
        rules.add(PolicyRule.builder()
                .name("Self-Employed Applicant Approval")
                .description("Approve self-employed/business applicants with stricter criteria")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.employmentType")
                                .operator(ConditionOperator.IN)
                                .values(List.of("SELF_EMPLOYED", "BUSINESS"))
                                .build(),
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("700")
                                .build(),
                        Condition.builder()
                                .field("applicant.age")
                                .operator(ConditionOperator.BETWEEN)
                                .minValue("25")
                                .maxValue("55")
                                .build(),
                        Condition.builder()
                                .field("applicant.monthlyIncome")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("40000")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.APPROVE)
                                .description("Eligible for personal loan — self-employed applicant")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_MAX_AMOUNT)
                                .parameters(Map.of("amount", "1500000"))
                                .description("Maximum loan amount: INR 15 lakhs")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_INTEREST_RATE)
                                .parameters(Map.of("rate", "14.0", "type", "FIXED"))
                                .description("Standard interest rate for self-employed applicants")
                                .build()
                ))
                .priority(20)
                .enabled(true)
                .build());

        return Policy.builder()
                .name("Personal Loan - Eligibility Template")
                .description("Pre-built eligibility template for Personal Loans. " +
                        "Covers salaried and self-employed approval, CIBIL-based rejection, and borderline referral rules.")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.PERSONAL_LOAN)
                .status(PolicyStatus.DRAFT)
                .versionNumber(1)
                .priority(100)
                .tags(new ArrayList<>(List.of("template", "personal-loan", "eligibility")))
                .rules(rules)
                .build();
    }

    /**
     * Home Loan Eligibility Template
     *
     * Rules:
     * 1. Low CIBIL Rejection (priority 5): cibilScore < 600 → REJECT
     * 2. Insufficient Income (priority 6): monthlyIncome < 40000 → REJECT
     * 3. High Value Referral (priority 10): requestedAmount > 50L → REFER
     * 4. Standard Approval (priority 20): cibilScore >= 700 + age 21-65 + income >= 40K + property → APPROVE
     */
    Policy buildHomeLoanEligibilityTemplate() {
        List<PolicyRule> rules = new ArrayList<>();

        // Rule 1: Low CIBIL Rejection
        rules.add(PolicyRule.builder()
                .name("Low CIBIL Rejection")
                .description("Reject applicants with CIBIL score below 600 for Home Loans")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.LESS_THAN)
                                .value("600")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REJECT)
                                .description("CIBIL score below Home Loan minimum threshold of 600")
                                .build()
                ))
                .priority(5)
                .enabled(true)
                .build());

        // Rule 2: Insufficient Income
        rules.add(PolicyRule.builder()
                .name("Insufficient Income Rejection")
                .description("Reject applicants with monthly income below INR 40,000")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.monthlyIncome")
                                .operator(ConditionOperator.LESS_THAN)
                                .value("40000")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REJECT)
                                .description("Monthly income below minimum requirement of INR 40,000")
                                .build()
                ))
                .priority(6)
                .enabled(true)
                .build());

        // Rule 3: High Value Referral
        rules.add(PolicyRule.builder()
                .name("High Value Loan Referral")
                .description("Refer loans above INR 50 lakhs to senior underwriter")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("loan.requestedAmount")
                                .operator(ConditionOperator.GREATER_THAN)
                                .value("5000000")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REFER)
                                .description("High value loan requires senior underwriter review")
                                .build(),
                        Action.builder()
                                .type(ActionType.ASSIGN_TO_ROLE)
                                .parameters(Map.of("role", "SENIOR_UNDERWRITER"))
                                .description("Assign to senior underwriter")
                                .build(),
                        Action.builder()
                                .type(ActionType.REQUIRE_DOCUMENT)
                                .parameters(Map.of("documentType", "VALUATION_REPORT", "mandatory", "true"))
                                .description("Require property valuation report for high-value loans")
                                .build()
                ))
                .priority(10)
                .enabled(true)
                .build());

        // Rule 4: Standard Home Loan Approval
        rules.add(PolicyRule.builder()
                .name("Standard Home Loan Approval")
                .description("Approve applicants meeting all Home Loan eligibility criteria")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("700")
                                .build(),
                        Condition.builder()
                                .field("applicant.age")
                                .operator(ConditionOperator.BETWEEN)
                                .minValue("21")
                                .maxValue("65")
                                .build(),
                        Condition.builder()
                                .field("applicant.monthlyIncome")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("40000")
                                .build(),
                        Condition.builder()
                                .field("property.estimatedValue")
                                .operator(ConditionOperator.IS_NOT_NULL)
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.APPROVE)
                                .description("Eligible for Home Loan")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_MAX_TENURE)
                                .parameters(Map.of("months", "360"))
                                .description("Maximum tenure: 30 years")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_INTEREST_RATE)
                                .parameters(Map.of("rate", "8.5", "type", "FLOATING"))
                                .description("Standard floating rate for Home Loans")
                                .build(),
                        Action.builder()
                                .type(ActionType.REQUIRE_DOCUMENT)
                                .parameters(Map.of("documentType", "PROPERTY_PAPERS", "mandatory", "true"))
                                .description("Require property ownership documents")
                                .build()
                ))
                .priority(20)
                .enabled(true)
                .build());

        return Policy.builder()
                .name("Home Loan - Eligibility Template")
                .description("Pre-built eligibility template for Home Loans. " +
                        "Covers standard approval, high-value referral, CIBIL rejection, and income check rules.")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.HOME_LOAN)
                .status(PolicyStatus.DRAFT)
                .versionNumber(1)
                .priority(100)
                .tags(new ArrayList<>(List.of("template", "home-loan", "eligibility")))
                .rules(rules)
                .build();
    }

    /**
     * KCC (Kisan Credit Card) Eligibility Template
     *
     * Rules:
     * 1. No Land Rejection (priority 5): landOwnership IS_FALSE → REJECT
     * 2. Large Farmer Enhanced (priority 10): landArea > 5 + irrigated → higher limits
     * 3. Standard KCC Approval (priority 20): landOwnership + cropType → APPROVE
     */
    Policy buildKccEligibilityTemplate() {
        List<PolicyRule> rules = new ArrayList<>();

        // Rule 1: No Land Ownership Rejection
        rules.add(PolicyRule.builder()
                .name("No Land Ownership Rejection")
                .description("Reject applicants without land ownership for KCC")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.landOwnership")
                                .operator(ConditionOperator.IS_FALSE)
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REJECT)
                                .description("Land ownership is required for KCC")
                                .build()
                ))
                .priority(5)
                .enabled(true)
                .build());

        // Rule 2: Large Farmer Enhanced Limits
        rules.add(PolicyRule.builder()
                .name("Large Farmer Enhanced Limit")
                .description("Enhanced credit limits for large farmers with irrigated land (>5 acres)")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.landArea")
                                .operator(ConditionOperator.GREATER_THAN)
                                .value("5")
                                .build(),
                        Condition.builder()
                                .field("applicant.irrigatedLand")
                                .operator(ConditionOperator.IS_TRUE)
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.SET_MAX_AMOUNT)
                                .parameters(Map.of("amount", "500000"))
                                .description("Enhanced limit: INR 5 lakhs for large farmers")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_INTEREST_RATE)
                                .parameters(Map.of("rate", "3.5", "type", "FIXED"))
                                .description("Preferential rate for large farmers")
                                .build()
                ))
                .priority(10)
                .enabled(true)
                .build());

        // Rule 3: Standard KCC Approval
        rules.add(PolicyRule.builder()
                .name("Standard KCC Approval")
                .description("Standard KCC approval for farmers with land and crop cultivation")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.landOwnership")
                                .operator(ConditionOperator.IS_TRUE)
                                .build(),
                        Condition.builder()
                                .field("applicant.cropType")
                                .operator(ConditionOperator.IS_NOT_NULL)
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.APPROVE)
                                .description("Eligible for Kisan Credit Card")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_MAX_AMOUNT)
                                .parameters(Map.of("amount", "300000"))
                                .description("Standard KCC limit: INR 3 lakhs")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_INTEREST_RATE)
                                .parameters(Map.of("rate", "4.0", "type", "FIXED"))
                                .description("Standard KCC interest rate (subsidized)")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_PROCESSING_FEE)
                                .parameters(Map.of("percentage", "0.5"))
                                .description("Minimal processing fee for KCC")
                                .build()
                ))
                .priority(20)
                .enabled(true)
                .build());

        return Policy.builder()
                .name("KCC - Eligibility Template")
                .description("Pre-built eligibility template for Kisan Credit Card (KCC). " +
                        "Covers standard KCC approval, large farmer enhanced limits, and land ownership rejection.")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.KCC)
                .status(PolicyStatus.DRAFT)
                .versionNumber(1)
                .priority(100)
                .tags(new ArrayList<>(List.of("template", "kcc", "kisan-credit-card", "eligibility", "agriculture")))
                .rules(rules)
                .build();
    }
}
