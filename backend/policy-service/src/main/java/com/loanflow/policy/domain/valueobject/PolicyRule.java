package com.loanflow.policy.domain.valueobject;

import com.loanflow.policy.domain.enums.LogicalOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A policy rule consists of conditions (joined by logical operator) and actions.
 * When conditions evaluate to true, the actions are executed.
 *
 * Example Rule: "Personal Loan Eligibility - Salaried"
 *   Conditions (AND):
 *     - employment.type EQUALS "SALARIED"
 *     - applicant.age BETWEEN 21, 58
 *     - income.monthly GREATER_THAN_OR_EQUAL 25000
 *   Actions:
 *     - APPROVE
 *     - SET_MAX_AMOUNT {amount: "2000000"}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRule {

    /**
     * Unique name for this rule within the policy
     */
    private String name;

    /**
     * Human-readable description
     */
    private String description;

    /**
     * How to combine conditions: AND (all must match) or OR (any must match)
     */
    @Builder.Default
    private LogicalOperator logicalOperator = LogicalOperator.AND;

    /**
     * List of conditions to evaluate
     */
    private List<Condition> conditions;

    /**
     * Actions to execute when conditions are met
     */
    private List<Action> actions;

    /**
     * Priority within the policy (lower = higher priority).
     * When multiple rules match, lower priority rules are applied first.
     */
    @Builder.Default
    private Integer priority = 100;

    /**
     * Whether this rule is enabled
     */
    @Builder.Default
    private Boolean enabled = true;
}
