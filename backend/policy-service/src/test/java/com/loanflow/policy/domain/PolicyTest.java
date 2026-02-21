package com.loanflow.policy.domain;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Policy aggregate root
 */
@DisplayName("Policy Aggregate Tests")
class PolicyTest {

    @Nested
    @DisplayName("Policy Creation")
    class PolicyCreation {

        @Test
        @DisplayName("Should create policy with defaults")
        void shouldCreatePolicyWithDefaults() {
            Policy policy = Policy.builder()
                    .name("Personal Loan Eligibility")
                    .category(PolicyCategory.ELIGIBILITY)
                    .loanType(LoanType.PERSONAL_LOAN)
                    .build();

            assertEquals("Personal Loan Eligibility", policy.getName());
            assertEquals(PolicyCategory.ELIGIBILITY, policy.getCategory());
            assertEquals(LoanType.PERSONAL_LOAN, policy.getLoanType());
            assertEquals(PolicyStatus.DRAFT, policy.getStatus());
            assertEquals(1, policy.getVersionNumber());
            assertEquals(100, policy.getPriority());
            assertNotNull(policy.getRules());
            assertTrue(policy.getRules().isEmpty());
        }

        @Test
        @DisplayName("Should generate unique policy code")
        void shouldGeneratePolicyCode() {
            String code1 = Policy.generatePolicyCode();
            String code2 = Policy.generatePolicyCode();

            assertNotNull(code1);
            assertTrue(code1.startsWith("POL-"));
            assertNotEquals(code1, code2);
        }

        @Test
        @DisplayName("Should run prePersist hook")
        void shouldRunPrePersist() {
            Policy policy = Policy.builder()
                    .name("Test Policy")
                    .category(PolicyCategory.PRICING)
                    .loanType(LoanType.HOME_LOAN)
                    .build();

            policy.prePersist();

            assertNotNull(policy.getPolicyCode());
            assertEquals(PolicyStatus.DRAFT, policy.getStatus());
            assertEquals(1, policy.getVersionNumber());
        }
    }

    @Nested
    @DisplayName("Policy Lifecycle")
    class PolicyLifecycle {

        @Test
        @DisplayName("Should activate policy with rules")
        void shouldActivatePolicyWithRules() {
            Policy policy = createPolicyWithRule();
            policy.activate();
            assertEquals(PolicyStatus.ACTIVE, policy.getStatus());
        }

        @Test
        @DisplayName("Should throw when activating policy without rules")
        void shouldThrowWhenActivatingWithoutRules() {
            Policy policy = Policy.builder()
                    .name("Empty Policy")
                    .category(PolicyCategory.ELIGIBILITY)
                    .loanType(LoanType.PERSONAL_LOAN)
                    .rules(new ArrayList<>())
                    .build();

            assertThrows(IllegalStateException.class, policy::activate);
        }

        @Test
        @DisplayName("Should not throw when activating already active policy")
        void shouldNotThrowWhenAlreadyActive() {
            Policy policy = createPolicyWithRule();
            policy.activate();
            assertDoesNotThrow(policy::activate);
            assertEquals(PolicyStatus.ACTIVE, policy.getStatus());
        }

        @Test
        @DisplayName("Should deactivate policy")
        void shouldDeactivatePolicy() {
            Policy policy = createPolicyWithRule();
            policy.activate();
            policy.deactivate();
            assertEquals(PolicyStatus.INACTIVE, policy.getStatus());
        }

        @Test
        @DisplayName("Should archive policy")
        void shouldArchivePolicy() {
            Policy policy = createPolicyWithRule();
            policy.activate();
            policy.archive();
            assertEquals(PolicyStatus.ARCHIVED, policy.getStatus());
        }
    }

    @Nested
    @DisplayName("Policy Versioning")
    class PolicyVersioning {

        @Test
        @DisplayName("Should create new version from existing policy")
        void shouldCreateNewVersion() {
            Policy original = createPolicyWithRule();
            original.setId("original-id");
            original.setPolicyCode("POL-2026-000001");
            original.setModifiedBy("admin@loanflow.com");

            Policy newVersion = original.createNewVersion();

            assertEquals("POL-2026-000001", newVersion.getPolicyCode());
            assertEquals(2, newVersion.getVersionNumber());
            assertEquals("original-id", newVersion.getPreviousVersionId());
            assertEquals(PolicyStatus.DRAFT, newVersion.getStatus());
            assertEquals(original.getName(), newVersion.getName());
            assertEquals(original.getCategory(), newVersion.getCategory());
            assertNotNull(newVersion.getRules());
            assertEquals(original.getRules().size(), newVersion.getRules().size());
        }

        @Test
        @DisplayName("New version should have independent rule list")
        void shouldHaveIndependentRuleList() {
            Policy original = createPolicyWithRule();
            original.setId("original-id");
            original.setModifiedBy("admin");

            Policy newVersion = original.createNewVersion();
            newVersion.getRules().clear();

            // Original should not be affected
            assertEquals(1, original.getRules().size());
            assertEquals(0, newVersion.getRules().size());
        }
    }

    @Nested
    @DisplayName("Rule Management")
    class RuleManagement {

        @Test
        @DisplayName("Should add rule to DRAFT policy")
        void shouldAddRuleToDraftPolicy() {
            Policy policy = Policy.builder()
                    .name("Test")
                    .status(PolicyStatus.DRAFT)
                    .build();

            PolicyRule rule = createSampleRule("Age Check");
            policy.addRule(rule);

            assertEquals(1, policy.getRuleCount());
        }

        @Test
        @DisplayName("Should throw when adding rule to ACTIVE policy")
        void shouldThrowWhenAddingRuleToActivePolicy() {
            Policy policy = createPolicyWithRule();
            policy.activate();

            PolicyRule newRule = createSampleRule("New Rule");
            assertThrows(IllegalStateException.class, () -> policy.addRule(newRule));
        }

        @Test
        @DisplayName("Should throw when adding rule to ARCHIVED policy")
        void shouldThrowWhenAddingRuleToArchivedPolicy() {
            Policy policy = createPolicyWithRule();
            policy.activate();
            policy.archive();

            PolicyRule newRule = createSampleRule("New Rule");
            assertThrows(IllegalStateException.class, () -> policy.addRule(newRule));
        }

        @Test
        @DisplayName("Should remove rule by name from DRAFT policy")
        void shouldRemoveRuleByName() {
            Policy policy = Policy.builder()
                    .name("Test")
                    .status(PolicyStatus.DRAFT)
                    .rules(new ArrayList<>(List.of(
                            createSampleRule("Rule A"),
                            createSampleRule("Rule B")
                    )))
                    .build();

            boolean removed = policy.removeRule("Rule A");
            assertTrue(removed);
            assertEquals(1, policy.getRuleCount());
            assertEquals("Rule B", policy.getRules().get(0).getName());
        }

        @Test
        @DisplayName("Should return false when removing non-existent rule")
        void shouldReturnFalseForNonExistentRule() {
            Policy policy = Policy.builder()
                    .name("Test")
                    .status(PolicyStatus.DRAFT)
                    .rules(new ArrayList<>(List.of(createSampleRule("Rule A"))))
                    .build();

            assertFalse(policy.removeRule("Non-existent"));
        }

        @Test
        @DisplayName("Should get enabled rules sorted by priority")
        void shouldGetEnabledRulesSortedByPriority() {
            PolicyRule rule1 = createSampleRule("Low Priority");
            rule1.setPriority(200);
            rule1.setEnabled(true);

            PolicyRule rule2 = createSampleRule("High Priority");
            rule2.setPriority(50);
            rule2.setEnabled(true);

            PolicyRule rule3 = createSampleRule("Disabled Rule");
            rule3.setEnabled(false);

            Policy policy = Policy.builder()
                    .name("Test")
                    .rules(new ArrayList<>(List.of(rule1, rule2, rule3)))
                    .build();

            List<PolicyRule> enabled = policy.getEnabledRules();
            assertEquals(2, enabled.size());
            assertEquals("High Priority", enabled.get(0).getName());
            assertEquals("Low Priority", enabled.get(1).getName());
        }
    }

    @Nested
    @DisplayName("Policy Effectiveness")
    class PolicyEffectiveness {

        @Test
        @DisplayName("Active policy with no date range should be effective")
        void activePolicyNoDatesShouldBeEffective() {
            Policy policy = createPolicyWithRule();
            policy.activate();
            assertTrue(policy.isEffective());
        }

        @Test
        @DisplayName("DRAFT policy should not be effective")
        void draftPolicyShouldNotBeEffective() {
            Policy policy = createPolicyWithRule();
            assertFalse(policy.isEffective());
        }

        @Test
        @DisplayName("Active policy within date range should be effective")
        void activePolicyWithinDateRangeShouldBeEffective() {
            Policy policy = createPolicyWithRule();
            policy.setEffectiveFrom(LocalDateTime.now().minusDays(1));
            policy.setEffectiveUntil(LocalDateTime.now().plusDays(1));
            policy.activate();
            assertTrue(policy.isEffective());
        }

        @Test
        @DisplayName("Active policy past effective date should not be effective")
        void activePolicyPastDateShouldNotBeEffective() {
            Policy policy = createPolicyWithRule();
            policy.setEffectiveUntil(LocalDateTime.now().minusDays(1));
            policy.activate();
            assertFalse(policy.isEffective());
        }

        @Test
        @DisplayName("Active policy before start date should not be effective")
        void activePolicyBeforeStartShouldNotBeEffective() {
            Policy policy = createPolicyWithRule();
            policy.setEffectiveFrom(LocalDateTime.now().plusDays(1));
            policy.activate();
            assertFalse(policy.isEffective());
        }
    }

    // ==================== Helper Methods ====================

    private Policy createPolicyWithRule() {
        return Policy.builder()
                .name("Test Policy")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.PERSONAL_LOAN)
                .status(PolicyStatus.DRAFT)
                .rules(new ArrayList<>(List.of(createSampleRule("Sample Rule"))))
                .build();
    }

    private PolicyRule createSampleRule(String name) {
        return PolicyRule.builder()
                .name(name)
                .description("Test rule")
                .logicalOperator(LogicalOperator.AND)
                .conditions(List.of(
                        Condition.builder()
                                .field("applicant.age")
                                .operator(ConditionOperator.GREATER_THAN_OR_EQUAL)
                                .value("21")
                                .build()
                ))
                .actions(List.of(
                        Action.builder()
                                .type(ActionType.APPROVE)
                                .description("Auto approve")
                                .parameters(Map.of())
                                .build()
                ))
                .priority(100)
                .enabled(true)
                .build();
    }
}
