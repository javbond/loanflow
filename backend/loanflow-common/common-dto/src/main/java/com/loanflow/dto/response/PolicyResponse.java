package com.loanflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for policy data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private String id;
    private String policyCode;
    private String name;
    private String description;
    private String category;
    private String loanType;
    private String status;
    private Integer versionNumber;
    private String previousVersionId;
    private Integer priority;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private List<String> tags;
    private List<PolicyRuleResponse> rules;
    private int ruleCount;
    private boolean effective;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Nested DTO for policy rules
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyRuleResponse {
        private String name;
        private String description;
        private String logicalOperator;
        private List<ConditionResponse> conditions;
        private List<ActionResponse> actions;
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
    public static class ConditionResponse {
        private String field;
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
    public static class ActionResponse {
        private String type;
        private Map<String, String> parameters;
        private String description;
    }
}
