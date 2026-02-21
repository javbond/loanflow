package com.loanflow.policy.evaluation.service.impl;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.LoanType;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.evaluation.dto.EvaluationContext;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationRequest;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.*;
import com.loanflow.policy.evaluation.engine.ActionResolver;
import com.loanflow.policy.evaluation.engine.RuleEvaluator;
import com.loanflow.policy.evaluation.service.PolicyEvaluationService;
import com.loanflow.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of PolicyEvaluationService.
 *
 * Evaluation flow:
 * 1. Build evaluation context from request
 * 2. Fetch active policies for the loan type (from cache or DB)
 * 3. Sort policies by priority
 * 4. For each policy, evaluate enabled rules against context
 * 5. Collect all triggered actions
 * 6. Resolve conflicts and determine overall decision
 * 7. Build and return evaluation response with audit trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyEvaluationServiceImpl implements PolicyEvaluationService {

    private final PolicyRepository policyRepository;
    private final RuleEvaluator ruleEvaluator;
    private final ActionResolver actionResolver;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_ACTIVE_PREFIX = "policy:active:entities:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public PolicyEvaluationResponse evaluate(PolicyEvaluationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting policy evaluation for application: {}, loanType: {}",
                request.getApplicationId(), request.getLoanType());

        List<EvaluationLogEntry> evaluationLog = new ArrayList<>();
        evaluationLog.add(EvaluationLogEntry.info(
                "Starting evaluation for application " + request.getApplicationId()));

        // Step 1: Build evaluation context
        EvaluationContext context = request.toEvaluationContext();
        evaluationLog.add(EvaluationLogEntry.info(
                "Evaluation context built with " + context.getData().size() + " fields"));

        // Step 2: Parse loan type
        LoanType loanType;
        try {
            loanType = LoanType.valueOf(request.getLoanType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid loan type: {}", request.getLoanType());
            return buildErrorResponse(request, "Invalid loan type: " + request.getLoanType(),
                    startTime, evaluationLog);
        }

        // Step 3: Fetch active policies (from cache or DB)
        List<Policy> activePolicies = new ArrayList<>(getActivePolicies(loanType));
        evaluationLog.add(EvaluationLogEntry.info(
                "Found " + activePolicies.size() + " active policies for " + loanType));

        if (activePolicies.isEmpty()) {
            log.info("No active policies found for loan type: {}", loanType);
            return buildNoMatchResponse(request, startTime, evaluationLog);
        }

        // Step 4: Sort policies by priority (lower = evaluated first)
        activePolicies.sort(Comparator.comparingInt(Policy::getPriority));

        // Step 5: Evaluate each policy
        List<PolicyMatchResult> matchResults = new ArrayList<>();
        List<TriggeredAction> allTriggeredActions = new ArrayList<>();
        int totalRulesEvaluated = 0;
        int totalRulesMatched = 0;

        for (Policy policy : activePolicies) {
            if (!policy.isEffective()) {
                evaluationLog.add(EvaluationLogEntry.info(
                        "Skipping policy " + policy.getPolicyCode() + " (not effective)"));
                continue;
            }

            PolicyMatchResult policyResult = evaluatePolicy(policy, context, evaluationLog);
            matchResults.add(policyResult);

            // Count rules
            totalRulesEvaluated += policyResult.getRuleResults().size();
            long matchedRules = policyResult.getRuleResults().stream()
                    .filter(RuleMatchResult::isMatched).count();
            totalRulesMatched += (int) matchedRules;

            // Collect triggered actions
            for (RuleMatchResult ruleResult : policyResult.getRuleResults()) {
                if (ruleResult.isMatched()) {
                    allTriggeredActions.addAll(ruleResult.getTriggeredActions());
                }
            }
        }

        // Step 6: Resolve actions and determine overall decision
        List<TriggeredAction> resolvedActions = actionResolver.resolveActions(allTriggeredActions);
        String overallDecision = actionResolver.resolveDecision(allTriggeredActions);

        long duration = System.currentTimeMillis() - startTime;
        int policiesMatched = (int) matchResults.stream().filter(PolicyMatchResult::isMatched).count();

        evaluationLog.add(EvaluationLogEntry.info(String.format(
                "Evaluation complete: decision=%s, policies=%d/%d matched, rules=%d/%d matched, duration=%dms",
                overallDecision, policiesMatched, activePolicies.size(),
                totalRulesMatched, totalRulesEvaluated, duration)));

        log.info("Policy evaluation complete for {}: decision={}, policies matched={}/{}, duration={}ms",
                request.getApplicationId(), overallDecision, policiesMatched, activePolicies.size(), duration);

        return PolicyEvaluationResponse.builder()
                .overallDecision(overallDecision)
                .applicationId(request.getApplicationId())
                .loanType(request.getLoanType())
                .policiesEvaluated(activePolicies.size())
                .policiesMatched(policiesMatched)
                .rulesEvaluated(totalRulesEvaluated)
                .rulesMatched(totalRulesMatched)
                .matchedPolicies(matchResults)
                .triggeredActions(resolvedActions)
                .evaluationLog(evaluationLog)
                .evaluatedAt(LocalDateTime.now())
                .evaluationDurationMs(duration)
                .build();
    }

    /**
     * Evaluate a single policy against the context
     */
    private PolicyMatchResult evaluatePolicy(Policy policy, EvaluationContext context,
                                              List<EvaluationLogEntry> evaluationLog) {
        log.debug("Evaluating policy: {} ({})", policy.getName(), policy.getPolicyCode());
        evaluationLog.add(EvaluationLogEntry.info(
                "Evaluating policy: " + policy.getName() + " [" + policy.getPolicyCode() + "]"));

        List<PolicyRule> enabledRules = policy.getEnabledRules();
        List<RuleMatchResult> ruleResults = new ArrayList<>();
        boolean anyRuleMatched = false;

        for (PolicyRule rule : enabledRules) {
            RuleMatchResult ruleResult = ruleEvaluator.evaluate(rule, context, policy.getPolicyCode());
            ruleResults.add(ruleResult);

            if (ruleResult.isMatched()) {
                anyRuleMatched = true;
                evaluationLog.add(EvaluationLogEntry.info(
                        "  Rule MATCHED: " + rule.getName() + " → " +
                                ruleResult.getTriggeredActions().size() + " actions"));
            } else {
                evaluationLog.add(EvaluationLogEntry.info(
                        "  Rule not matched: " + rule.getName()));
            }
        }

        return PolicyMatchResult.builder()
                .policyId(policy.getId())
                .policyCode(policy.getPolicyCode())
                .policyName(policy.getName())
                .category(policy.getCategory() != null ? policy.getCategory().name() : null)
                .priority(policy.getPriority())
                .matched(anyRuleMatched)
                .ruleResults(ruleResults)
                .build();
    }

    /**
     * Fetch active policies for a loan type, with Redis caching.
     * Caches the Policy entities directly (not PolicyResponse DTOs).
     */
    @SuppressWarnings("unchecked")
    private List<Policy> getActivePolicies(LoanType loanType) {
        String cacheKey = CACHE_ACTIVE_PREFIX + loanType.name();

        // Try cache first
        try {
            List<Policy> cached = (List<Policy>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for active policy entities: {}", loanType);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for {}: {}", cacheKey, e.getMessage());
        }

        // Fetch from DB
        List<Policy> policies = policyRepository.findActivePoliciesForLoanType(loanType);

        // Cache the result
        try {
            redisTemplate.opsForValue().set(cacheKey, policies, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache active policy entities for {}: {}", loanType, e.getMessage());
        }

        return policies;
    }

    /**
     * Build response for no matching policies
     */
    private PolicyEvaluationResponse buildNoMatchResponse(PolicyEvaluationRequest request,
                                                           long startTime,
                                                           List<EvaluationLogEntry> evaluationLog) {
        evaluationLog.add(EvaluationLogEntry.warn("No active policies found - evaluation skipped"));
        return PolicyEvaluationResponse.builder()
                .overallDecision("NO_MATCH")
                .applicationId(request.getApplicationId())
                .loanType(request.getLoanType())
                .policiesEvaluated(0)
                .policiesMatched(0)
                .rulesEvaluated(0)
                .rulesMatched(0)
                .evaluationLog(evaluationLog)
                .evaluatedAt(LocalDateTime.now())
                .evaluationDurationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Build error response
     */
    private PolicyEvaluationResponse buildErrorResponse(PolicyEvaluationRequest request,
                                                         String errorMessage,
                                                         long startTime,
                                                         List<EvaluationLogEntry> evaluationLog) {
        evaluationLog.add(EvaluationLogEntry.warn("Evaluation error: " + errorMessage));
        return PolicyEvaluationResponse.builder()
                .overallDecision("ERROR")
                .applicationId(request.getApplicationId())
                .loanType(request.getLoanType())
                .policiesEvaluated(0)
                .policiesMatched(0)
                .evaluationLog(evaluationLog)
                .evaluatedAt(LocalDateTime.now())
                .evaluationDurationMs(System.currentTimeMillis() - startTime)
                .build();
    }
}
