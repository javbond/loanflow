package com.loanflow.policy.evaluation.service;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationRequest;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse;
import com.loanflow.policy.evaluation.engine.ActionResolver;
import com.loanflow.policy.evaluation.engine.ConditionEvaluator;
import com.loanflow.policy.evaluation.engine.RuleEvaluator;
import com.loanflow.policy.evaluation.service.impl.PolicyEvaluationServiceImpl;
import com.loanflow.policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for PolicyEvaluationServiceImpl.
 * Tests the orchestrator with mocked repository and Redis, but real evaluators.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyEvaluationService Tests")
class PolicyEvaluationServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PolicyEvaluationServiceImpl evaluationService;

    @BeforeEach
    void setUp() {
        // Use real evaluators (not mocks) for integration-style testing
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        RuleEvaluator ruleEvaluator = new RuleEvaluator(conditionEvaluator);
        ActionResolver actionResolver = new ActionResolver();

        evaluationService = new PolicyEvaluationServiceImpl(
                policyRepository, ruleEvaluator, actionResolver, redisTemplate);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null); // Cache miss
    }

    @Nested
    @DisplayName("Full Evaluation Flow")
    class FullEvaluationFlow {

        @Test
        @DisplayName("Should APPROVE eligible personal loan application")
        void shouldApproveEligibleApplication() {
            // Given: Active eligibility policy
            Policy eligibilityPolicy = createEligibilityPolicy();
            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of(eligibilityPolicy));

            // When: Evaluate a strong applicant
            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-001")
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(BigDecimal.valueOf(500000))
                    .tenureMonths(36)
                    .cibilScore(750)
                    .applicantAge(35)
                    .employmentType("SALARIED")
                    .monthlyIncome(BigDecimal.valueOf(85000))
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            // Then
            assertEquals("APPROVED", response.getOverallDecision());
            assertEquals("APP-001", response.getApplicationId());
            assertEquals(1, response.getPoliciesEvaluated());
            assertTrue(response.getPoliciesMatched() > 0);
            assertTrue(response.getRulesMatched() > 0);
            assertFalse(response.getTriggeredActions().isEmpty());
            assertFalse(response.getEvaluationLog().isEmpty());
            assertTrue(response.getEvaluationDurationMs() >= 0);
        }

        @Test
        @DisplayName("Should REJECT when CIBIL score is too low")
        void shouldRejectLowCibilScore() {
            // Given: Policy with rejection rule for low CIBIL
            Policy policy = createPolicyWithRejectionRule();
            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of(policy));

            // When: Evaluate applicant with low CIBIL
            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-002")
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(BigDecimal.valueOf(300000))
                    .tenureMonths(24)
                    .cibilScore(400)
                    .applicantAge(30)
                    .employmentType("SALARIED")
                    .monthlyIncome(BigDecimal.valueOf(50000))
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            // Then: Should be rejected
            assertEquals("REJECTED", response.getOverallDecision());
            assertTrue(response.getTriggeredActions().stream()
                    .anyMatch(a -> "REJECT".equals(a.getActionType())));
        }

        @Test
        @DisplayName("Should REFER when CIBIL is borderline")
        void shouldReferBorderlineApplication() {
            // Given: Policy that refers borderline cases
            Policy policy = createPolicyWithReferRule();
            when(policyRepository.findActivePoliciesForLoanType(LoanType.HOME_LOAN))
                    .thenReturn(List.of(policy));

            // When: Evaluate borderline applicant
            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-003")
                    .loanType("HOME_LOAN")
                    .requestedAmount(BigDecimal.valueOf(3000000))
                    .tenureMonths(240)
                    .cibilScore(650)
                    .applicantAge(40)
                    .employmentType("SALARIED")
                    .monthlyIncome(BigDecimal.valueOf(75000))
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            assertEquals("REFERRED", response.getOverallDecision());
        }
    }

    @Nested
    @DisplayName("No Match Scenarios")
    class NoMatchScenarios {

        @Test
        @DisplayName("Should return NO_MATCH when no active policies exist")
        void shouldReturnNoMatchWhenNoPolicies() {
            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of());

            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-004")
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(BigDecimal.valueOf(100000))
                    .tenureMonths(12)
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            assertEquals("NO_MATCH", response.getOverallDecision());
            assertEquals(0, response.getPoliciesEvaluated());
            assertEquals(0, response.getPoliciesMatched());
        }

        @Test
        @DisplayName("Should return ERROR for invalid loan type")
        void shouldReturnErrorForInvalidLoanType() {
            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-005")
                    .loanType("INVALID_TYPE")
                    .requestedAmount(BigDecimal.valueOf(100000))
                    .tenureMonths(12)
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            assertEquals("ERROR", response.getOverallDecision());
        }
    }

    @Nested
    @DisplayName("Multi-Policy Evaluation")
    class MultiPolicyEvaluation {

        @Test
        @DisplayName("Should evaluate multiple policies and resolve conflicts")
        void shouldEvaluateMultiplePolicies() {
            // Given: Two policies - eligibility (approve) + pricing
            Policy eligibilityPolicy = createEligibilityPolicy();
            eligibilityPolicy.setPriority(10);

            Policy pricingPolicy = createPricingPolicy();
            pricingPolicy.setPriority(20);

            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of(eligibilityPolicy, pricingPolicy));

            // When
            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-006")
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(BigDecimal.valueOf(500000))
                    .tenureMonths(36)
                    .cibilScore(750)
                    .applicantAge(35)
                    .employmentType("SALARIED")
                    .monthlyIncome(BigDecimal.valueOf(85000))
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            // Then: Both policies evaluated
            assertEquals("APPROVED", response.getOverallDecision());
            assertEquals(2, response.getPoliciesEvaluated());

            // Should have interest rate and max amount actions
            assertTrue(response.getTriggeredActions().stream()
                    .anyMatch(a -> "SET_INTEREST_RATE".equals(a.getActionType())));
        }

        @Test
        @DisplayName("Should handle REJECT overriding APPROVE from different policies")
        void shouldHandleRejectOverridingApprove() {
            // Given: One policy approves, another rejects (REJECT wins)
            Policy approvePolicy = createEligibilityPolicy();
            approvePolicy.setPriority(10);

            Policy rejectPolicy = createPolicyWithRejectionRule();
            rejectPolicy.setPriority(5); // Higher priority

            // Make the reject condition match the strong applicant too
            // by adjusting the CIBIL threshold
            rejectPolicy.getRules().get(0).getConditions().get(0).setValue("800");
            rejectPolicy.getRules().get(0).getConditions().get(0).setOperator(ConditionOperator.LESS_THAN);

            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of(approvePolicy, rejectPolicy));

            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-007")
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(BigDecimal.valueOf(500000))
                    .tenureMonths(36)
                    .cibilScore(750)  // Below 800 → reject rule matches
                    .applicantAge(35)
                    .employmentType("SALARIED")
                    .monthlyIncome(BigDecimal.valueOf(85000))
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            // REJECT takes precedence over APPROVE
            assertEquals("REJECTED", response.getOverallDecision());
        }
    }

    @Nested
    @DisplayName("Audit Trail")
    class AuditTrail {

        @Test
        @DisplayName("Should include detailed evaluation log")
        void shouldIncludeEvaluationLog() {
            Policy policy = createEligibilityPolicy();
            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of(policy));

            PolicyEvaluationRequest request = PolicyEvaluationRequest.builder()
                    .applicationId("APP-008")
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(BigDecimal.valueOf(500000))
                    .tenureMonths(36)
                    .cibilScore(750)
                    .applicantAge(35)
                    .employmentType("SALARIED")
                    .monthlyIncome(BigDecimal.valueOf(85000))
                    .build();

            PolicyEvaluationResponse response = evaluationService.evaluate(request);

            assertNotNull(response.getEvaluationLog());
            assertFalse(response.getEvaluationLog().isEmpty());
            // Should contain starting, context, policy, and completion log entries
            assertTrue(response.getEvaluationLog().size() >= 4);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Create an eligibility policy that approves salaried applicants with CIBIL >= 650, age 21-58
     */
    private Policy createEligibilityPolicy() {
        List<PolicyRule> rules = new ArrayList<>();
        rules.add(PolicyRule.builder()
                .name("Salaried Eligibility")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("650")
                                .build(),
                        Condition.builder()
                                .field("applicant.age")
                                .operator(ConditionOperator.BETWEEN)
                                .minValue("21")
                                .maxValue("58")
                                .build(),
                        Condition.builder()
                                .field("applicant.employmentType")
                                .operator(ConditionOperator.IN)
                                .values(List.of("SALARIED", "PROFESSIONAL"))
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.APPROVE)
                                .description("Eligible for personal loan")
                                .build(),
                        Action.builder()
                                .type(ActionType.SET_MAX_AMOUNT)
                                .parameters(Map.of("amount", "2000000"))
                                .description("Max 20 lakhs for salaried")
                                .build()
                ))
                .priority(10)
                .enabled(true)
                .build());

        return Policy.builder()
                .id("policy-eligibility-1")
                .policyCode("POL-ELIG-001")
                .name("Personal Loan Eligibility")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.PERSONAL_LOAN)
                .status(PolicyStatus.ACTIVE)
                .rules(rules)
                .priority(10)
                .build();
    }

    /**
     * Create a policy with rejection rule for low CIBIL
     */
    private Policy createPolicyWithRejectionRule() {
        List<PolicyRule> rules = new ArrayList<>();
        rules.add(PolicyRule.builder()
                .name("Low CIBIL Rejection")
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
                                .description("CIBIL score below minimum threshold")
                                .build()
                ))
                .priority(1)
                .enabled(true)
                .build());

        return Policy.builder()
                .id("policy-reject-1")
                .policyCode("POL-REJ-001")
                .name("Auto-Rejection Policy")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.PERSONAL_LOAN)
                .status(PolicyStatus.ACTIVE)
                .rules(rules)
                .priority(5)
                .build();
    }

    /**
     * Create a policy with refer rule for borderline cases
     */
    private Policy createPolicyWithReferRule() {
        List<PolicyRule> rules = new ArrayList<>();
        rules.add(PolicyRule.builder()
                .name("Borderline CIBIL Review")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.BETWEEN)
                                .minValue("600")
                                .maxValue("699")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.REFER)
                                .description("Borderline CIBIL - refer to senior underwriter")
                                .build(),
                        Action.builder()
                                .type(ActionType.ASSIGN_TO_ROLE)
                                .parameters(Map.of("role", "SENIOR_UNDERWRITER"))
                                .build()
                ))
                .priority(5)
                .enabled(true)
                .build());

        return Policy.builder()
                .id("policy-refer-1")
                .policyCode("POL-REF-001")
                .name("Borderline Review Policy")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.HOME_LOAN)
                .status(PolicyStatus.ACTIVE)
                .rules(rules)
                .priority(10)
                .build();
    }

    /**
     * Create a pricing policy that sets interest rate
     */
    private Policy createPricingPolicy() {
        List<PolicyRule> rules = new ArrayList<>();
        rules.add(PolicyRule.builder()
                .name("Premium Rate")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.cibilScore")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("700")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.SET_INTEREST_RATE)
                                .parameters(Map.of("rate", "10.5"))
                                .description("Preferential rate for high CIBIL")
                                .build()
                ))
                .priority(10)
                .enabled(true)
                .build());

        return Policy.builder()
                .id("policy-pricing-1")
                .policyCode("POL-PRICE-001")
                .name("Personal Loan Pricing")
                .category(PolicyCategory.PRICING)
                .loanType(LoanType.PERSONAL_LOAN)
                .status(PolicyStatus.ACTIVE)
                .rules(rules)
                .priority(20)
                .build();
    }
}
