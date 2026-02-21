package com.loanflow.policy.evaluation.engine;

import com.loanflow.policy.domain.enums.LogicalOperator;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.evaluation.dto.EvaluationContext;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.ConditionResult;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.RuleMatchResult;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.TriggeredAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a complete policy rule (conditions + actions) against an evaluation context.
 * Handles AND/OR logical operators for combining condition results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluator {

    private final ConditionEvaluator conditionEvaluator;

    /**
     * Evaluate a policy rule against the context.
     *
     * @param rule        the policy rule to evaluate
     * @param context     the evaluation context
     * @param policyCode  the source policy code (for action tracking)
     * @return RuleMatchResult with condition details and triggered actions
     */
    public RuleMatchResult evaluate(PolicyRule rule, EvaluationContext context, String policyCode) {
        log.debug("Evaluating rule: '{}' (operator: {})", rule.getName(), rule.getLogicalOperator());

        List<ConditionResult> conditionResults = new ArrayList<>();

        // Evaluate all conditions
        if (rule.getConditions() != null) {
            for (var condition : rule.getConditions()) {
                ConditionResult result = conditionEvaluator.evaluate(condition, context);
                conditionResults.add(result);
            }
        }

        // Determine if the rule matches based on logical operator
        boolean ruleMatched = evaluateLogicalResult(rule.getLogicalOperator(), conditionResults);

        // Build triggered actions if rule matched
        List<TriggeredAction> triggeredActions = new ArrayList<>();
        if (ruleMatched && rule.getActions() != null) {
            for (var action : rule.getActions()) {
                triggeredActions.add(TriggeredAction.builder()
                        .actionType(action.getType().name())
                        .parameters(action.getParameters())
                        .description(action.getDescription())
                        .sourcePolicyCode(policyCode)
                        .sourceRuleName(rule.getName())
                        .priority(rule.getPriority() != null ? rule.getPriority() : 100)
                        .build());
            }
        }

        log.debug("Rule '{}' evaluation result: {} ({} conditions, {} matched)",
                rule.getName(), ruleMatched ? "MATCHED" : "NOT MATCHED",
                conditionResults.size(),
                conditionResults.stream().filter(ConditionResult::isMatched).count());

        return RuleMatchResult.builder()
                .ruleName(rule.getName())
                .matched(ruleMatched)
                .logicalOperator(rule.getLogicalOperator() != null ? rule.getLogicalOperator().name() : "AND")
                .conditionResults(conditionResults)
                .triggeredActions(triggeredActions)
                .build();
    }

    /**
     * Evaluate combined condition results using logical operator.
     * AND: all conditions must match
     * OR: at least one condition must match
     * Empty conditions list → matches (vacuously true)
     */
    private boolean evaluateLogicalResult(LogicalOperator operator, List<ConditionResult> results) {
        if (results.isEmpty()) {
            return true; // No conditions = always matches
        }

        if (operator == null || operator == LogicalOperator.AND) {
            return results.stream().allMatch(ConditionResult::isMatched);
        } else {
            return results.stream().anyMatch(ConditionResult::isMatched);
        }
    }
}
