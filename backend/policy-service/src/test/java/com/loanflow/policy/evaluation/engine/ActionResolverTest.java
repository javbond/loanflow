package com.loanflow.policy.evaluation.engine;

import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse.TriggeredAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD unit tests for ActionResolver.
 * Tests decision resolution, action deduplication, and conflict resolution.
 */
@DisplayName("ActionResolver Tests")
class ActionResolverTest {

    private ActionResolver actionResolver;

    @BeforeEach
    void setUp() {
        actionResolver = new ActionResolver();
    }

    @Nested
    @DisplayName("Decision Resolution")
    class DecisionResolution {

        @Test
        @DisplayName("Should return REJECTED when REJECT action is present")
        void shouldReturnRejectedWhenRejectPresent() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder().actionType("APPROVE").priority(10).build(),
                    TriggeredAction.builder().actionType("REJECT").priority(20).build()
            );

            String decision = actionResolver.resolveDecision(actions);

            assertEquals("REJECTED", decision);
        }

        @Test
        @DisplayName("Should return REFERRED when REFER action is present (no REJECT)")
        void shouldReturnReferredWhenReferPresent() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder().actionType("APPROVE").priority(10).build(),
                    TriggeredAction.builder().actionType("REFER").priority(20).build()
            );

            String decision = actionResolver.resolveDecision(actions);

            assertEquals("REFERRED", decision);
        }

        @Test
        @DisplayName("Should return REFERRED when FLAG_RISK action is present")
        void shouldReturnReferredWhenFlagRiskPresent() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder().actionType("APPROVE").priority(10).build(),
                    TriggeredAction.builder().actionType("FLAG_RISK").priority(20).build()
            );

            String decision = actionResolver.resolveDecision(actions);

            assertEquals("REFERRED", decision);
        }

        @Test
        @DisplayName("Should return APPROVED when only APPROVE action present")
        void shouldReturnApprovedWhenOnlyApprovePresent() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder().actionType("APPROVE").priority(10).build(),
                    TriggeredAction.builder().actionType("SET_INTEREST_RATE")
                            .parameters(Map.of("rate", "12.5")).priority(20).build()
            );

            String decision = actionResolver.resolveDecision(actions);

            assertEquals("APPROVED", decision);
        }

        @Test
        @DisplayName("Should return NO_MATCH for empty actions")
        void shouldReturnNoMatchForEmptyActions() {
            String decision = actionResolver.resolveDecision(List.of());

            assertEquals("NO_MATCH", decision);
        }

        @Test
        @DisplayName("Should return NO_DECISION for non-decision actions only")
        void shouldReturnNoDecisionForNonDecisionActionsOnly() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder().actionType("SET_INTEREST_RATE")
                            .parameters(Map.of("rate", "12.5")).priority(10).build(),
                    TriggeredAction.builder().actionType("REQUIRE_DOCUMENT")
                            .parameters(Map.of("documentType", "SALARY_SLIP")).priority(20).build()
            );

            String decision = actionResolver.resolveDecision(actions);

            assertEquals("NO_DECISION", decision);
        }
    }

    @Nested
    @DisplayName("Action Conflict Resolution")
    class ActionConflictResolution {

        @Test
        @DisplayName("Should deduplicate SET_INTEREST_RATE - highest priority wins")
        void shouldDeduplicateSettingActions() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder()
                            .actionType("SET_INTEREST_RATE")
                            .parameters(Map.of("rate", "12.5"))
                            .sourcePolicyCode("POL-001")
                            .priority(10)
                            .build(),
                    TriggeredAction.builder()
                            .actionType("SET_INTEREST_RATE")
                            .parameters(Map.of("rate", "15.0"))
                            .sourcePolicyCode("POL-002")
                            .priority(20)
                            .build()
            );

            List<TriggeredAction> resolved = actionResolver.resolveActions(actions);

            // Only one SET_INTEREST_RATE should remain (priority 10)
            long interestRateActions = resolved.stream()
                    .filter(a -> "SET_INTEREST_RATE".equals(a.getActionType()))
                    .count();
            assertEquals(1, interestRateActions);

            TriggeredAction winner = resolved.stream()
                    .filter(a -> "SET_INTEREST_RATE".equals(a.getActionType()))
                    .findFirst().orElseThrow();
            assertEquals("12.5", winner.getParameters().get("rate"));
            assertEquals("POL-001", winner.getSourcePolicyCode());
        }

        @Test
        @DisplayName("Should keep all REQUIRE_DOCUMENT actions (accumulating)")
        void shouldKeepAllAccumulatingActions() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder()
                            .actionType("REQUIRE_DOCUMENT")
                            .parameters(Map.of("documentType", "SALARY_SLIP"))
                            .priority(10)
                            .build(),
                    TriggeredAction.builder()
                            .actionType("REQUIRE_DOCUMENT")
                            .parameters(Map.of("documentType", "BANK_STATEMENT"))
                            .priority(20)
                            .build()
            );

            List<TriggeredAction> resolved = actionResolver.resolveActions(actions);

            long docActions = resolved.stream()
                    .filter(a -> "REQUIRE_DOCUMENT".equals(a.getActionType()))
                    .count();
            assertEquals(2, docActions);
        }

        @Test
        @DisplayName("Should return actions sorted by priority")
        void shouldReturnActionsSortedByPriority() {
            List<TriggeredAction> actions = List.of(
                    TriggeredAction.builder().actionType("APPROVE").priority(30).build(),
                    TriggeredAction.builder().actionType("SET_INTEREST_RATE")
                            .parameters(Map.of("rate", "10")).priority(10).build(),
                    TriggeredAction.builder().actionType("REQUIRE_DOCUMENT")
                            .parameters(Map.of("documentType", "ID")).priority(20).build()
            );

            List<TriggeredAction> resolved = actionResolver.resolveActions(actions);

            assertEquals(3, resolved.size());
            assertEquals(10, resolved.get(0).getPriority());
            assertEquals(20, resolved.get(1).getPriority());
            assertEquals(30, resolved.get(2).getPriority());
        }
    }
}
