package com.loanflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating/updating a policy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String name;

    private String description;

    @NotNull(message = "Policy category is required")
    private String category; // PolicyCategory enum value

    @NotNull(message = "Loan type is required")
    private String loanType; // LoanType enum value

    private Integer priority;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveUntil;

    private List<String> tags;

    private List<PolicyRuleRequest> rules;

    /**
     * Nested DTO for policy rules
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyRuleRequest {
        @NotBlank(message = "Rule name is required")
        private String name;
        private String description;
        private String logicalOperator; // AND or OR
        private List<ConditionRequest> conditions;
        private List<ActionRequest> actions;
        private Integer priority;
        private Boolean enabled;
    }

    /**
     * Nested DTO for conditions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionRequest {
        @NotBlank(message = "Field is required")
        private String field;
        @NotBlank(message = "Operator is required")
        private String operator;
        private String value;
        private List<String> values;
        private String minValue;
        private String maxValue;
    }

    /**
     * Nested DTO for actions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionRequest {
        @NotBlank(message = "Action type is required")
        private String type;
        private Map<String, String> parameters;
        private String description;
    }
}
