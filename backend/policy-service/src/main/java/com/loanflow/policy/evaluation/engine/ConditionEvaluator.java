package com.loanflow.policy.evaluation.engine;

import com.loanflow.policy.domain.enums.ConditionOperator;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.evaluation.dto.EvaluationContext;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.ConditionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evaluates individual policy conditions against an evaluation context.
 *
 * Supports all 15 ConditionOperator types:
 * - Comparison: EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL
 * - Collection: IN, NOT_IN
 * - Range: BETWEEN
 * - String: CONTAINS, STARTS_WITH
 * - Boolean: IS_TRUE, IS_FALSE
 * - Null: IS_NULL, IS_NOT_NULL
 */
@Component
@Slf4j
public class ConditionEvaluator {

    /**
     * Evaluate a single condition against the context.
     *
     * @param condition the condition to evaluate
     * @param context   the evaluation context with field values
     * @return ConditionResult with match status and details
     */
    public ConditionResult evaluate(Condition condition, EvaluationContext context) {
        String field = condition.getField();
        ConditionOperator operator = condition.getOperator();
        String actualValue = context.get(field);

        log.debug("Evaluating condition: {} {} {}", field, operator, condition.getValue());

        // Handle null-check operators first (they don't need an actual value)
        if (operator == ConditionOperator.IS_NULL) {
            boolean matched = actualValue == null || actualValue.isBlank();
            return buildResult(condition, actualValue, matched,
                    matched ? "Field is null/empty" : "Field has value: " + actualValue);
        }

        if (operator == ConditionOperator.IS_NOT_NULL) {
            boolean matched = actualValue != null && !actualValue.isBlank();
            return buildResult(condition, actualValue, matched,
                    matched ? "Field has value: " + actualValue : "Field is null/empty");
        }

        // For all other operators, field must exist in context
        if (actualValue == null) {
            return buildResult(condition, null, false,
                    "Field '" + field + "' not found in evaluation context");
        }

        try {
            boolean matched = evaluateOperator(operator, actualValue, condition);
            String reason = matched
                    ? String.format("'%s' %s %s → PASS", actualValue, operator, getExpectedValueDisplay(condition))
                    : String.format("'%s' %s %s → FAIL", actualValue, operator, getExpectedValueDisplay(condition));
            return buildResult(condition, actualValue, matched, reason);
        } catch (Exception e) {
            log.warn("Error evaluating condition {} {} {}: {}",
                    field, operator, condition.getValue(), e.getMessage());
            return buildResult(condition, actualValue, false,
                    "Evaluation error: " + e.getMessage());
        }
    }

    /**
     * Core operator evaluation logic
     */
    private boolean evaluateOperator(ConditionOperator operator, String actualValue, Condition condition) {
        return switch (operator) {
            case EQUALS -> evaluateEquals(actualValue, condition.getValue());
            case NOT_EQUALS -> !evaluateEquals(actualValue, condition.getValue());
            case GREATER_THAN -> compareNumeric(actualValue, condition.getValue()) > 0;
            case GREATER_THAN_OR_EQUAL -> compareNumeric(actualValue, condition.getValue()) >= 0;
            case LESS_THAN -> compareNumeric(actualValue, condition.getValue()) < 0;
            case LESS_THAN_OR_EQUAL -> compareNumeric(actualValue, condition.getValue()) <= 0;
            case IN -> evaluateIn(actualValue, condition.getValues());
            case NOT_IN -> !evaluateIn(actualValue, condition.getValues());
            case BETWEEN -> evaluateBetween(actualValue, condition.getMinValue(), condition.getMaxValue());
            case CONTAINS -> actualValue.toLowerCase().contains(condition.getValue().toLowerCase());
            case STARTS_WITH -> actualValue.toLowerCase().startsWith(condition.getValue().toLowerCase());
            case IS_TRUE -> evaluateBoolean(actualValue, true);
            case IS_FALSE -> evaluateBoolean(actualValue, false);
            case IS_NULL, IS_NOT_NULL -> throw new IllegalStateException("Null operators handled earlier");
        };
    }

    /**
     * Compare equality — tries numeric comparison first, falls back to string (case-insensitive)
     */
    private boolean evaluateEquals(String actual, String expected) {
        if (actual == null || expected == null) return false;

        // Try numeric comparison first
        Double actualNum = parseNumber(actual);
        Double expectedNum = parseNumber(expected);
        if (actualNum != null && expectedNum != null) {
            return Double.compare(actualNum, expectedNum) == 0;
        }

        // Fall back to case-insensitive string comparison
        return actual.trim().equalsIgnoreCase(expected.trim());
    }

    /**
     * Numeric comparison
     */
    private int compareNumeric(String actual, String expected) {
        Double actualNum = parseNumber(actual);
        Double expectedNum = parseNumber(expected);

        if (actualNum == null) {
            throw new IllegalArgumentException("Cannot parse '" + actual + "' as a number for comparison");
        }
        if (expectedNum == null) {
            throw new IllegalArgumentException("Cannot parse '" + expected + "' as a number for comparison");
        }

        return Double.compare(actualNum, expectedNum);
    }

    /**
     * Check if actual value is in the list of expected values
     */
    private boolean evaluateIn(String actualValue, List<String> values) {
        if (values == null || values.isEmpty()) return false;

        // Try numeric comparison first
        Double actualNum = parseNumber(actualValue);
        if (actualNum != null) {
            return values.stream()
                    .map(this::parseNumber)
                    .anyMatch(v -> v != null && Double.compare(v, actualNum) == 0);
        }

        // Fall back to case-insensitive string comparison
        return values.stream()
                .anyMatch(v -> v != null && v.trim().equalsIgnoreCase(actualValue.trim()));
    }

    /**
     * Check if value is between min and max (inclusive)
     */
    private boolean evaluateBetween(String actualValue, String minValue, String maxValue) {
        Double actual = parseNumber(actualValue);
        Double min = parseNumber(minValue);
        Double max = parseNumber(maxValue);

        if (actual == null) {
            throw new IllegalArgumentException("Cannot parse '" + actualValue + "' as a number for BETWEEN");
        }
        if (min == null) {
            throw new IllegalArgumentException("Cannot parse min value '" + minValue + "' as a number");
        }
        if (max == null) {
            throw new IllegalArgumentException("Cannot parse max value '" + maxValue + "' as a number");
        }

        return actual >= min && actual <= max;
    }

    /**
     * Boolean comparison
     */
    private boolean evaluateBoolean(String actual, boolean expected) {
        if ("true".equalsIgnoreCase(actual) || "1".equals(actual) || "yes".equalsIgnoreCase(actual)) {
            return expected;
        }
        if ("false".equalsIgnoreCase(actual) || "0".equals(actual) || "no".equalsIgnoreCase(actual)) {
            return !expected;
        }
        throw new IllegalArgumentException("Cannot parse '" + actual + "' as a boolean");
    }

    /**
     * Parse a string as a number, returning null if not parseable
     */
    private Double parseNumber(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Build a ConditionResult
     */
    private ConditionResult buildResult(Condition condition, String actualValue, boolean matched, String reason) {
        return ConditionResult.builder()
                .field(condition.getField())
                .operator(condition.getOperator().name())
                .expectedValue(getExpectedValueDisplay(condition))
                .actualValue(actualValue)
                .matched(matched)
                .reason(reason)
                .build();
    }

    /**
     * Get display string for the expected value (for logging/audit)
     */
    private String getExpectedValueDisplay(Condition condition) {
        if (condition.getOperator() == ConditionOperator.BETWEEN) {
            return "[" + condition.getMinValue() + ", " + condition.getMaxValue() + "]";
        }
        if (condition.getOperator() == ConditionOperator.IN || condition.getOperator() == ConditionOperator.NOT_IN) {
            return condition.getValues() != null ? condition.getValues().toString() : "[]";
        }
        if (condition.getOperator() == ConditionOperator.IS_NULL || condition.getOperator() == ConditionOperator.IS_NOT_NULL
                || condition.getOperator() == ConditionOperator.IS_TRUE || condition.getOperator() == ConditionOperator.IS_FALSE) {
            return "";
        }
        return condition.getValue() != null ? condition.getValue() : "";
    }
}
