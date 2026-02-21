package com.loanflow.policy.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for policy evaluation results.
 * Contains the overall decision, matched policies, triggered actions, and audit trail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationResponse {

    /**
     * Overall evaluation decision: APPROVED, REJECTED, REFERRED, or NO_MATCH
     */
    private String overallDecision;

    /**
     * Application ID that was evaluated
     */
    private String applicationId;

    /**
     * Loan type that was evaluated
     */
    private String loanType;

    /**
     * Number of policies evaluated
     */
    private int policiesEvaluated;

    /**
     * Number of policies that matched (had at least one rule match)
     */
    private int policiesMatched;

    /**
     * Total rules evaluated across all policies
     */
    private int rulesEvaluated;

    /**
     * Total rules that matched
     */
    private int rulesMatched;

    /**
     * Details of each matched policy and its rule results
     */
    @Builder.Default
    private List<PolicyMatchResult> matchedPolicies = new ArrayList<>();

    /**
     * All triggered actions from matching rules (deduplicated, priority-ordered)
     */
    @Builder.Default
    private List<TriggeredAction> triggeredActions = new ArrayList<>();

    /**
     * Evaluation log entries for audit trail
     */
    @Builder.Default
    private List<EvaluationLogEntry> evaluationLog = new ArrayList<>();

    /**
     * Timestamp of evaluation
     */
    @Builder.Default
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    /**
     * Duration of evaluation in milliseconds
     */
    private long evaluationDurationMs;

    // ==================== Nested Result Classes ====================

    /**
     * Result for a single policy evaluation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyMatchResult {
        private String policyId;
        private String policyCode;
        private String policyName;
        private String category;
        private int priority;
        private boolean matched;
        @Builder.Default
        private List<RuleMatchResult> ruleResults = new ArrayList<>();
    }

    /**
     * Result for a single rule evaluation within a policy
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleMatchResult {
        private String ruleName;
        private boolean matched;
        private String logicalOperator;
        @Builder.Default
        private List<ConditionResult> conditionResults = new ArrayList<>();
        @Builder.Default
        private List<TriggeredAction> triggeredActions = new ArrayList<>();
    }

    /**
     * Result for a single condition evaluation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionResult {
        private String field;
        private String operator;
        private String expectedValue;
        private String actualValue;
        private boolean matched;
        private String reason;
    }

    /**
     * An action triggered by a matching rule
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggeredAction {
        private String actionType;
        private Map<String, String> parameters;
        private String description;
        private String sourcePolicyCode;
        private String sourceRuleName;
        private int priority;
    }

    /**
     * Audit log entry for evaluation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationLogEntry {
        private String level;  // INFO, WARN, ERROR
        private String message;
        private LocalDateTime timestamp;

        public static EvaluationLogEntry info(String message) {
            return EvaluationLogEntry.builder()
                    .level("INFO")
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        public static EvaluationLogEntry warn(String message) {
            return EvaluationLogEntry.builder()
                    .level("WARN")
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
