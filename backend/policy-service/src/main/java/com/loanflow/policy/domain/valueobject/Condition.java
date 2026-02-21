package com.loanflow.policy.domain.valueobject;

import com.loanflow.policy.domain.enums.ConditionOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single condition in a policy rule.
 * Example: "applicant.age >= 21" or "creditScore IN [700, 750, 800]"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Condition {

    /**
     * The field/attribute to evaluate.
     * Uses dot notation for nested fields.
     * Examples: "applicant.age", "loan.amount", "creditScore", "employment.type"
     */
    private String field;

    /**
     * The comparison operator
     */
    private ConditionOperator operator;

    /**
     * The value to compare against (for single-value operators)
     */
    private String value;

    /**
     * List of values (for IN, NOT_IN operators)
     */
    private List<String> values;

    /**
     * Minimum value (for BETWEEN operator)
     */
    private String minValue;

    /**
     * Maximum value (for BETWEEN operator)
     */
    private String maxValue;
}
