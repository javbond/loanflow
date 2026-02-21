package com.loanflow.policy.evaluation.engine;

import com.loanflow.policy.domain.enums.ActionType;
import com.loanflow.policy.domain.enums.ConditionOperator;
import com.loanflow.policy.domain.enums.LogicalOperator;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.evaluation.dto.EvaluationContext;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.RuleMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD unit tests for RuleEvaluator.
 * Tests AND/OR logic, action triggering, and edge cases.
 */
@DisplayName("RuleEvaluator Tests")
class RuleEvaluatorTest {

    private RuleEvaluator ruleEvaluator;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        ruleEvaluator = new RuleEvaluator(conditionEvaluator);

        context = new EvaluationContext();
        context.put("applicant.cibilScore", "750");
        context.put("applicant.age", "35");
        context.put("applicant.employmentType", "SALARIED");
        context.put("applicant.monthlyIncome", "85000");
        context.put("loan.requestedAmount", "500000");
    }

    @Nested
    @DisplayName("AND Logic")
    class AndLogic {

        @Test
        @DisplayName("Should match when ALL conditions pass (AND)")
        void shouldMatchWhenAllConditionsPass() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Eligibility Check")
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
                                    .operator(ConditionOperator.EQUALS)
                                    .value("SALARIED")
                                    .build()
                    ))
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.APPROVE)
                                    .description("Auto-approve eligible application")
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertTrue(result.isMatched());
            assertEquals(3, result.getConditionResults().size());
            assertTrue(result.getConditionResults().stream().allMatch(c -> c.isMatched()));
            assertEquals(1, result.getTriggeredActions().size());
            assertEquals("APPROVE", result.getTriggeredActions().get(0).getActionType());
        }

        @Test
        @DisplayName("Should NOT match when ANY condition fails (AND)")
        void shouldNotMatchWhenAnyConditionFails() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Eligibility Check")
                    .logicalOperator(LogicalOperator.AND)
                    .conditions(List.of(
                            Condition.builder()
                                    .field("applicant.cibilScore")
                                    .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                    .value("800")  // 750 < 800, will fail
                                    .build(),
                            Condition.builder()
                                    .field("applicant.age")
                                    .operator(ConditionOperator.BETWEEN)
                                    .minValue("21")
                                    .maxValue("58")
                                    .build()
                    ))
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.APPROVE)
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertFalse(result.isMatched());
            assertEquals(0, result.getTriggeredActions().size());
        }
    }

    @Nested
    @DisplayName("OR Logic")
    class OrLogic {

        @Test
        @DisplayName("Should match when ANY condition passes (OR)")
        void shouldMatchWhenAnyConditionPasses() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Income OR Employment Check")
                    .logicalOperator(LogicalOperator.OR)
                    .conditions(List.of(
                            Condition.builder()
                                    .field("applicant.monthlyIncome")
                                    .operator(ConditionOperator.GREATER_THAN)
                                    .value("100000")  // 85000 < 100000, fails
                                    .build(),
                            Condition.builder()
                                    .field("applicant.employmentType")
                                    .operator(ConditionOperator.EQUALS)
                                    .value("SALARIED")  // matches!
                                    .build()
                    ))
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.SET_MAX_AMOUNT)
                                    .parameters(Map.of("amount", "2000000"))
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertTrue(result.isMatched());
            assertEquals(1, result.getTriggeredActions().size());
        }

        @Test
        @DisplayName("Should NOT match when ALL conditions fail (OR)")
        void shouldNotMatchWhenAllConditionsFail() {
            PolicyRule rule = PolicyRule.builder()
                    .name("High Income Check")
                    .logicalOperator(LogicalOperator.OR)
                    .conditions(List.of(
                            Condition.builder()
                                    .field("applicant.monthlyIncome")
                                    .operator(ConditionOperator.GREATER_THAN)
                                    .value("200000")  // fails
                                    .build(),
                            Condition.builder()
                                    .field("applicant.employmentType")
                                    .operator(ConditionOperator.EQUALS)
                                    .value("BUSINESS")  // fails
                                    .build()
                    ))
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.APPROVE)
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertFalse(result.isMatched());
        }
    }

    @Nested
    @DisplayName("Action Triggering")
    class ActionTriggering {

        @Test
        @DisplayName("Should trigger multiple actions when rule matches")
        void shouldTriggerMultipleActions() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Premium Applicant")
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
                                    .type(ActionType.APPROVE)
                                    .description("Auto-approve premium applicant")
                                    .build(),
                            Action.builder()
                                    .type(ActionType.SET_INTEREST_RATE)
                                    .parameters(Map.of("rate", "10.5"))
                                    .description("Preferential rate for high CIBIL")
                                    .build(),
                            Action.builder()
                                    .type(ActionType.SET_MAX_AMOUNT)
                                    .parameters(Map.of("amount", "3000000"))
                                    .description("Higher limit for premium customers")
                                    .build()
                    ))
                    .priority(10)
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-PREMIUM-001");

            assertTrue(result.isMatched());
            assertEquals(3, result.getTriggeredActions().size());

            // Verify action details
            assertEquals("POL-PREMIUM-001", result.getTriggeredActions().get(0).getSourcePolicyCode());
            assertEquals("Premium Applicant", result.getTriggeredActions().get(0).getSourceRuleName());
            assertEquals(10, result.getTriggeredActions().get(0).getPriority());
        }

        @Test
        @DisplayName("Should NOT trigger actions when rule does not match")
        void shouldNotTriggerActionsWhenNotMatched() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Rejection Rule")
                    .conditions(List.of(
                            Condition.builder()
                                    .field("applicant.cibilScore")
                                    .operator(ConditionOperator.LESS_THAN)
                                    .value("400")  // 750 > 400, will fail
                                    .build()
                    ))
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.REJECT)
                                    .description("Low CIBIL rejection")
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertFalse(result.isMatched());
            assertEquals(0, result.getTriggeredActions().size());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should match rule with empty conditions list")
        void shouldMatchEmptyConditions() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Default Rule")
                    .conditions(List.of())
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.NOTIFY)
                                    .parameters(Map.of("message", "Application received"))
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertTrue(result.isMatched());
            assertEquals(1, result.getTriggeredActions().size());
        }

        @Test
        @DisplayName("Should match rule with null conditions list")
        void shouldMatchNullConditions() {
            PolicyRule rule = PolicyRule.builder()
                    .name("Default Rule")
                    .conditions(null)
                    .actions(List.of(
                            Action.builder()
                                    .type(ActionType.NOTIFY)
                                    .build()
                    ))
                    .build();

            RuleMatchResult result = ruleEvaluator.evaluate(rule, context, "POL-TEST-001");

            assertTrue(result.isMatched());
        }
    }
}
