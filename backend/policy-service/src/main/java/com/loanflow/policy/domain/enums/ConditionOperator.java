package com.loanflow.policy.domain.enums;

/**
 * Operators for policy condition evaluation
 */
public enum ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IN,              // Value is in a list
    NOT_IN,          // Value is not in a list
    BETWEEN,         // Value is between min and max
    CONTAINS,        // String contains
    STARTS_WITH,     // String starts with
    IS_TRUE,         // Boolean true
    IS_FALSE,        // Boolean false
    IS_NULL,         // Value is null
    IS_NOT_NULL      // Value is not null
}
