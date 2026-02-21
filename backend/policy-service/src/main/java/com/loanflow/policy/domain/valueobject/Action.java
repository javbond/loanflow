package com.loanflow.policy.domain.valueobject;

import com.loanflow.policy.domain.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * An action to be executed when a policy rule matches.
 * Example: SET_INTEREST_RATE with parameters {rate: "12.5"}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    /**
     * Type of action to perform
     */
    private ActionType type;

    /**
     * Action parameters (key-value pairs).
     * Examples:
     *   SET_INTEREST_RATE → {rate: "12.5", type: "FIXED"}
     *   REQUIRE_DOCUMENT → {documentType: "SALARY_SLIP", mandatory: "true"}
     *   ASSIGN_TO_ROLE   → {role: "SENIOR_UNDERWRITER"}
     *   SET_MAX_AMOUNT   → {amount: "5000000"}
     */
    private Map<String, String> parameters;

    /**
     * Human-readable description of this action
     */
    private String description;
}
