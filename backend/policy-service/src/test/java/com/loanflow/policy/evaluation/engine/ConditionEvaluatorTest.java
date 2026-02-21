package com.loanflow.policy.evaluation.engine;

import com.loanflow.policy.domain.enums.ConditionOperator;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.evaluation.dto.EvaluationContext;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.ConditionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD unit tests for ConditionEvaluator.
 * Tests all 15 condition operators with various scenarios.
 */
@DisplayName("ConditionEvaluator Tests")
class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
        context = new EvaluationContext();
        // Set up a realistic evaluation context
        context.put("applicant.cibilScore", "750");
        context.put("applicant.age", "35");
        context.put("applicant.employmentType", "SALARIED");
        context.put("applicant.monthlyIncome", "85000");
        context.put("applicant.yearsOfExperience", "10");
        context.put("loan.type", "PERSONAL_LOAN");
        context.put("loan.requestedAmount", "500000");
        context.put("loan.tenureMonths", "36");
        context.put("property.estimatedValue", "5000000");
        context.put("applicant.kycVerified", "true");
    }

    @Nested
    @DisplayName("EQUALS Operator")
    class EqualsOperator {

        @Test
        @DisplayName("Should match when string values are equal (case-insensitive)")
        void shouldMatchEqualStrings() {
            Condition condition = Condition.builder()
                    .field("applicant.employmentType")
                    .operator(ConditionOperator.EQUALS)
                    .value("SALARIED")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
            assertEquals("applicant.employmentType", result.getField());
            assertEquals("SALARIED", result.getActualValue());
        }

        @Test
        @DisplayName("Should match when numeric values are equal")
        void shouldMatchEqualNumbers() {
            Condition condition = Condition.builder()
                    .field("applicant.cibilScore")
                    .operator(ConditionOperator.EQUALS)
                    .value("750")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("Should not match when values differ")
        void shouldNotMatchDifferentValues() {
            Condition condition = Condition.builder()
                    .field("applicant.employmentType")
                    .operator(ConditionOperator.EQUALS)
                    .value("SELF_EMPLOYED")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
        }
    }

    @Nested
    @DisplayName("NOT_EQUALS Operator")
    class NotEqualsOperator {

        @Test
        @DisplayName("Should match when values differ")
        void shouldMatchDifferentValues() {
            Condition condition = Condition.builder()
                    .field("applicant.employmentType")
                    .operator(ConditionOperator.NOT_EQUALS)
                    .value("SELF_EMPLOYED")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }
    }

    @Nested
    @DisplayName("Comparison Operators (>, >=, <, <=)")
    class ComparisonOperators {

        @Test
        @DisplayName("GREATER_THAN should match when actual > expected")
        void greaterThanShouldMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.cibilScore")
                    .operator(ConditionOperator.GREATER_THAN)
                    .value("700")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("GREATER_THAN should not match when actual <= expected")
        void greaterThanShouldNotMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.cibilScore")
                    .operator(ConditionOperator.GREATER_THAN)
                    .value("750")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
        }

        @Test
        @DisplayName("GREATER_THAN_OR_EQUAL should match when actual == expected")
        void greaterThanOrEqualShouldMatchEqual() {
            Condition condition = Condition.builder()
                    .field("applicant.cibilScore")
                    .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                    .value("750")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("LESS_THAN should match when actual < expected")
        void lessThanShouldMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.age")
                    .operator(ConditionOperator.LESS_THAN)
                    .value("60")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("LESS_THAN_OR_EQUAL should match when actual <= expected")
        void lessThanOrEqualShouldMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.age")
                    .operator(ConditionOperator.LESS_THAN_OR_EQUAL)
                    .value("35")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }
    }

    @Nested
    @DisplayName("IN / NOT_IN Operators")
    class InOperators {

        @Test
        @DisplayName("IN should match when value is in the list")
        void inShouldMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.employmentType")
                    .operator(ConditionOperator.IN)
                    .values(List.of("SALARIED", "PROFESSIONAL"))
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("IN should not match when value is not in the list")
        void inShouldNotMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.employmentType")
                    .operator(ConditionOperator.IN)
                    .values(List.of("SELF_EMPLOYED", "BUSINESS"))
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
        }

        @Test
        @DisplayName("NOT_IN should match when value is not in the list")
        void notInShouldMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.employmentType")
                    .operator(ConditionOperator.NOT_IN)
                    .values(List.of("SELF_EMPLOYED", "BUSINESS"))
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }
    }

    @Nested
    @DisplayName("BETWEEN Operator")
    class BetweenOperator {

        @Test
        @DisplayName("Should match when value is within range (inclusive)")
        void shouldMatchWithinRange() {
            Condition condition = Condition.builder()
                    .field("applicant.age")
                    .operator(ConditionOperator.BETWEEN)
                    .minValue("21")
                    .maxValue("58")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("Should match at boundary values")
        void shouldMatchAtBoundary() {
            // Set age to exactly 21
            context.put("applicant.age", "21");

            Condition condition = Condition.builder()
                    .field("applicant.age")
                    .operator(ConditionOperator.BETWEEN)
                    .minValue("21")
                    .maxValue("58")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("Should not match when value is outside range")
        void shouldNotMatchOutsideRange() {
            context.put("applicant.age", "19");

            Condition condition = Condition.builder()
                    .field("applicant.age")
                    .operator(ConditionOperator.BETWEEN)
                    .minValue("21")
                    .maxValue("58")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
        }
    }

    @Nested
    @DisplayName("String Operators (CONTAINS, STARTS_WITH)")
    class StringOperators {

        @Test
        @DisplayName("CONTAINS should match substring (case-insensitive)")
        void containsShouldMatch() {
            context.put("loan.purpose", "Home renovation and extension");

            Condition condition = Condition.builder()
                    .field("loan.purpose")
                    .operator(ConditionOperator.CONTAINS)
                    .value("renovation")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("STARTS_WITH should match prefix (case-insensitive)")
        void startsWithShouldMatch() {
            context.put("loan.purpose", "Home renovation");

            Condition condition = Condition.builder()
                    .field("loan.purpose")
                    .operator(ConditionOperator.STARTS_WITH)
                    .value("home")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }
    }

    @Nested
    @DisplayName("Boolean Operators (IS_TRUE, IS_FALSE)")
    class BooleanOperators {

        @Test
        @DisplayName("IS_TRUE should match when field is true")
        void isTrueShouldMatch() {
            Condition condition = Condition.builder()
                    .field("applicant.kycVerified")
                    .operator(ConditionOperator.IS_TRUE)
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("IS_FALSE should not match when field is true")
        void isFalseShouldNotMatchTrue() {
            Condition condition = Condition.builder()
                    .field("applicant.kycVerified")
                    .operator(ConditionOperator.IS_FALSE)
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
        }
    }

    @Nested
    @DisplayName("Null Operators (IS_NULL, IS_NOT_NULL)")
    class NullOperators {

        @Test
        @DisplayName("IS_NULL should match when field does not exist")
        void isNullShouldMatchMissingField() {
            Condition condition = Condition.builder()
                    .field("applicant.nonExistentField")
                    .operator(ConditionOperator.IS_NULL)
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("IS_NOT_NULL should match when field exists")
        void isNotNullShouldMatchExistingField() {
            Condition condition = Condition.builder()
                    .field("applicant.cibilScore")
                    .operator(ConditionOperator.IS_NOT_NULL)
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("IS_NOT_NULL should not match when field is missing")
        void isNotNullShouldNotMatchMissingField() {
            Condition condition = Condition.builder()
                    .field("applicant.nonExistent")
                    .operator(ConditionOperator.IS_NOT_NULL)
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle missing field gracefully")
        void shouldHandleMissingField() {
            Condition condition = Condition.builder()
                    .field("nonexistent.field")
                    .operator(ConditionOperator.EQUALS)
                    .value("something")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertFalse(result.isMatched());
            assertTrue(result.getReason().contains("not found"));
        }

        @Test
        @DisplayName("Should handle numeric comparison with decimal values")
        void shouldHandleDecimalComparison() {
            context.put("loan.interestRate", "12.5");

            Condition condition = Condition.builder()
                    .field("loan.interestRate")
                    .operator(ConditionOperator.LESS_THAN_OR_EQUAL)
                    .value("15.0")
                    .build();

            ConditionResult result = evaluator.evaluate(condition, context);

            assertTrue(result.isMatched());
        }
    }
}
