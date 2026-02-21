package com.loanflow.policy.evaluation.engine;

import com.loanflow.policy.domain.enums.ActionType;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.TriggeredAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves triggered actions from multiple matching rules/policies.
 *
 * Responsibilities:
 * - Deduplicates conflicting actions (e.g., multiple SET_INTEREST_RATE)
 * - Determines overall decision from APPROVE/REJECT/REFER actions
 * - Orders actions by priority (lower = higher priority)
 *
 * Decision Priority (most conservative wins):
 * REJECT > REFER > APPROVE
 */
@Component
@Slf4j
public class ActionResolver {

    /**
     * Resolve the overall decision from all triggered actions.
     *
     * Decision logic:
     * - If any REJECT action → REJECTED
     * - Else if any REFER action → REFERRED
     * - Else if any APPROVE action → APPROVED
     * - Else → NO_DECISION (no decision-making actions triggered)
     *
     * @param actions all triggered actions from matching rules
     * @return the overall decision string
     */
    public String resolveDecision(List<TriggeredAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return "NO_MATCH";
        }

        Set<String> actionTypes = actions.stream()
                .map(TriggeredAction::getActionType)
                .collect(Collectors.toSet());

        // Most conservative decision wins
        if (actionTypes.contains(ActionType.REJECT.name())) {
            log.info("Decision: REJECTED (reject action triggered)");
            return "REJECTED";
        }
        if (actionTypes.contains(ActionType.REFER.name())) {
            log.info("Decision: REFERRED (refer action triggered)");
            return "REFERRED";
        }
        if (actionTypes.contains(ActionType.FLAG_RISK.name())) {
            log.info("Decision: REFERRED (risk flag triggered)");
            return "REFERRED";
        }
        if (actionTypes.contains(ActionType.APPROVE.name())) {
            log.info("Decision: APPROVED (approve action triggered)");
            return "APPROVED";
        }

        log.info("Decision: NO_DECISION (no decision actions in triggered set)");
        return "NO_DECISION";
    }

    /**
     * Deduplicate and resolve conflicting actions.
     * For actions that set values (SET_INTEREST_RATE, SET_MAX_AMOUNT, etc.),
     * the highest-priority (lowest number) action wins.
     *
     * @param actions all triggered actions (may contain duplicates/conflicts)
     * @return deduplicated list of actions, ordered by priority
     */
    public List<TriggeredAction> resolveActions(List<TriggeredAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }

        // Group actions by type
        Map<String, List<TriggeredAction>> byType = actions.stream()
                .collect(Collectors.groupingBy(TriggeredAction::getActionType));

        List<TriggeredAction> resolved = new ArrayList<>();

        for (Map.Entry<String, List<TriggeredAction>> entry : byType.entrySet()) {
            String actionType = entry.getKey();
            List<TriggeredAction> typeActions = entry.getValue();

            if (isSettingAction(actionType)) {
                // For setting actions, take the highest priority (lowest number)
                typeActions.stream()
                        .min(Comparator.comparingInt(TriggeredAction::getPriority))
                        .ifPresent(resolved::add);

                if (typeActions.size() > 1) {
                    log.debug("Resolved conflicting {} actions: {} candidates, picked priority {}",
                            actionType, typeActions.size(),
                            resolved.get(resolved.size() - 1).getPriority());
                }
            } else if (isAccumulatingAction(actionType)) {
                // For accumulating actions (REQUIRE_DOCUMENT, NOTIFY), keep all
                resolved.addAll(typeActions);
            } else {
                // For decision actions (APPROVE, REJECT, REFER), keep highest priority
                typeActions.stream()
                        .min(Comparator.comparingInt(TriggeredAction::getPriority))
                        .ifPresent(resolved::add);
            }
        }

        // Sort by priority (lowest = first)
        resolved.sort(Comparator.comparingInt(TriggeredAction::getPriority));
        return resolved;
    }

    /**
     * Check if this is a "setting" action that should be deduplicated
     * (only one value can be set, highest priority wins)
     */
    private boolean isSettingAction(String actionType) {
        return ActionType.SET_INTEREST_RATE.name().equals(actionType)
                || ActionType.SET_PROCESSING_FEE.name().equals(actionType)
                || ActionType.SET_MAX_AMOUNT.name().equals(actionType)
                || ActionType.SET_MAX_TENURE.name().equals(actionType)
                || ActionType.ASSIGN_TO_ROLE.name().equals(actionType);
    }

    /**
     * Check if this is an "accumulating" action where all instances should be kept
     */
    private boolean isAccumulatingAction(String actionType) {
        return ActionType.REQUIRE_DOCUMENT.name().equals(actionType)
                || ActionType.NOTIFY.name().equals(actionType);
    }
}
