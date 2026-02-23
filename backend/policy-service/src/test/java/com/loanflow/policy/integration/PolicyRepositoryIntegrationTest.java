package com.loanflow.policy.integration;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.repository.PolicyRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PolicyRepository with embedded MongoDB.
 * Tests CRUD operations, custom queries, policy versioning,
 * and active policy retrieval against an actual MongoDB instance.
 */
@DataMongoTest
@ActiveProfiles("integration-test")
class PolicyRepositoryIntegrationTest {

    @Autowired
    private PolicyRepository policyRepository;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
    }

    // ==================== Test Data Builders ====================

    private Policy buildPolicy(String name, PolicyCategory category,
                                LoanType loanType, PolicyStatus status) {
        Policy policy = new Policy();
        policy.setName(name);
        policy.setDescription("Test policy: " + name);
        policy.setCategory(category);
        policy.setLoanType(loanType);
        policy.setStatus(status);
        policy.setPriority(100);
        policy.setVersionNumber(1);
        policy.setTags(List.of("test", category.name().toLowerCase()));
        policy.setCreatedBy("test-user");
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        policy.setPolicyCode(Policy.generatePolicyCode());
        return policy;
    }

    private PolicyRule buildRule(String name) {
        Condition condition = new Condition();
        condition.setField("requestedAmount");
        condition.setOperator(ConditionOperator.GREATER_THAN);
        condition.setValue("100000");

        Action action = new Action();
        action.setType(ActionType.SET_INTEREST_RATE);
        action.setParameters(Map.of("rate", "12.5", "type", "FIXED"));
        action.setDescription("Set interest rate for large loans");

        PolicyRule rule = new PolicyRule();
        rule.setName(name);
        rule.setDescription("Test rule");
        rule.setEnabled(true);
        rule.setPriority(1);
        rule.setConditions(List.of(condition));
        rule.setActions(List.of(action));
        return rule;
    }

    // ==================== CRUD Tests ====================

    @Nested
    @DisplayName("Basic CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and retrieve policy by ID")
        void shouldSaveAndRetrieve() {
            Policy policy = buildPolicy("Home Loan Eligibility",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.DRAFT);

            Policy saved = policyRepository.save(policy);

            Optional<Policy> found = policyRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Home Loan Eligibility");
            assertThat(found.get().getCategory()).isEqualTo(PolicyCategory.ELIGIBILITY);
            assertThat(found.get().getStatus()).isEqualTo(PolicyStatus.DRAFT);
        }

        @Test
        @DisplayName("Should find policy by policy code")
        void shouldFindByPolicyCode() {
            Policy policy = buildPolicy("Personal Loan Pricing",
                    PolicyCategory.PRICING, LoanType.PERSONAL_LOAN, PolicyStatus.ACTIVE);
            Policy saved = policyRepository.save(policy);

            Optional<Policy> found = policyRepository.findFirstByPolicyCodeOrderByVersionNumberDesc(
                    saved.getPolicyCode());

            assertThat(found).isPresent();
            assertThat(found.get().getPolicyCode()).isEqualTo(saved.getPolicyCode());
        }

        @Test
        @DisplayName("Should check name uniqueness (case-insensitive)")
        void shouldCheckNameUniqueness() {
            policyRepository.save(buildPolicy("Unique Policy Name",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.DRAFT));

            boolean exists = policyRepository.existsByNameIgnoreCase("unique policy name");
            boolean notExists = policyRepository.existsByNameIgnoreCase("non-existent");

            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    // ==================== Query Tests ====================

    @Nested
    @DisplayName("Custom Queries")
    class QueryTests {

        @Test
        @DisplayName("Should find active policies for specific loan type")
        void shouldFindActivePoliciesForLoanType() {
            policyRepository.save(buildPolicy("Home Eligibility",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ACTIVE));
            policyRepository.save(buildPolicy("Home Pricing",
                    PolicyCategory.PRICING, LoanType.HOME_LOAN, PolicyStatus.ACTIVE));
            policyRepository.save(buildPolicy("Personal Eligibility",
                    PolicyCategory.ELIGIBILITY, LoanType.PERSONAL_LOAN, PolicyStatus.ACTIVE));
            policyRepository.save(buildPolicy("Draft Policy",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.DRAFT));

            List<Policy> homePolicies = policyRepository.findActivePoliciesForLoanType(LoanType.HOME_LOAN);

            assertThat(homePolicies).hasSize(2);
            assertThat(homePolicies).allMatch(p -> p.getStatus() == PolicyStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should include ALL loan type in active policies query")
        void shouldIncludeAllLoanType() {
            policyRepository.save(buildPolicy("Global Policy",
                    PolicyCategory.ELIGIBILITY, LoanType.ALL, PolicyStatus.ACTIVE));
            policyRepository.save(buildPolicy("Home Specific",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ACTIVE));

            List<Policy> homePolicies = policyRepository.findActivePoliciesForLoanType(LoanType.HOME_LOAN);

            assertThat(homePolicies).hasSize(2);
        }

        @Test
        @DisplayName("Should find policies by category and status")
        void shouldFindByCategoryAndStatus() {
            policyRepository.save(buildPolicy("Eligibility 1",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ACTIVE));
            policyRepository.save(buildPolicy("Eligibility 2",
                    PolicyCategory.ELIGIBILITY, LoanType.PERSONAL_LOAN, PolicyStatus.ACTIVE));
            policyRepository.save(buildPolicy("Pricing 1",
                    PolicyCategory.PRICING, LoanType.HOME_LOAN, PolicyStatus.ACTIVE));

            List<Policy> eligibility = policyRepository.findByCategoryAndStatus(
                    PolicyCategory.ELIGIBILITY, PolicyStatus.ACTIVE);

            assertThat(eligibility).hasSize(2);
            assertThat(eligibility).allMatch(p -> p.getCategory() == PolicyCategory.ELIGIBILITY);
        }

        @Test
        @DisplayName("Should find by category with pagination")
        void shouldFindByCategoryPaginated() {
            for (int i = 0; i < 5; i++) {
                policyRepository.save(buildPolicy("Eligibility " + i,
                        PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ACTIVE));
            }

            Page<Policy> page = policyRepository.findByCategory(
                    PolicyCategory.ELIGIBILITY, PageRequest.of(0, 3));

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }

    // ==================== Versioning Tests ====================

    @Nested
    @DisplayName("Policy Versioning")
    class VersioningTests {

        @Test
        @DisplayName("Should retrieve version history ordered by version number DESC")
        void shouldGetVersionHistory() {
            String policyCode = "POL-TEST-000001";

            Policy v1 = buildPolicy("Evolving Policy",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ARCHIVED);
            v1.setPolicyCode(policyCode);
            v1.setVersionNumber(1);
            policyRepository.save(v1);

            Policy v2 = buildPolicy("Evolving Policy",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ACTIVE);
            v2.setPolicyCode(policyCode);
            v2.setVersionNumber(2);
            v2.setPreviousVersionId(v1.getId());
            policyRepository.save(v2);

            List<Policy> history = policyRepository.findByPolicyCodeOrderByVersionNumberDesc(policyCode);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).getVersionNumber()).isEqualTo(2);
            assertThat(history.get(1).getVersionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should find latest version by policy code")
        void shouldFindLatestVersion() {
            String policyCode = "POL-TEST-000002";

            Policy v1 = buildPolicy("Version Test",
                    PolicyCategory.PRICING, LoanType.PERSONAL_LOAN, PolicyStatus.ARCHIVED);
            v1.setPolicyCode(policyCode);
            v1.setVersionNumber(1);
            policyRepository.save(v1);

            Policy v2 = buildPolicy("Version Test",
                    PolicyCategory.PRICING, LoanType.PERSONAL_LOAN, PolicyStatus.ACTIVE);
            v2.setPolicyCode(policyCode);
            v2.setVersionNumber(2);
            policyRepository.save(v2);

            Optional<Policy> latest = policyRepository.findFirstByPolicyCodeOrderByVersionNumberDesc(policyCode);

            assertThat(latest).isPresent();
            assertThat(latest.get().getVersionNumber()).isEqualTo(2);
        }
    }

    // ==================== Rules Tests ====================

    @Nested
    @DisplayName("Policy with Rules")
    class RulesTests {

        @Test
        @DisplayName("Should save and retrieve policy with rules, conditions, and actions")
        void shouldSavePolicyWithRules() {
            Policy policy = buildPolicy("Loan Amount Risk Policy",
                    PolicyCategory.CREDIT_LIMIT, LoanType.HOME_LOAN, PolicyStatus.DRAFT);
            policy.setRules(List.of(
                    buildRule("High Amount Rule"),
                    buildRule("Medium Amount Rule")
            ));

            Policy saved = policyRepository.save(policy);
            Optional<Policy> found = policyRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getRules()).hasSize(2);
            assertThat(found.get().getRules().get(0).getConditions()).hasSize(1);
            assertThat(found.get().getRules().get(0).getActions()).hasSize(1);
        }
    }

    // ==================== Tag Tests ====================

    @Nested
    @DisplayName("Tag-based Queries")
    class TagTests {

        @Test
        @DisplayName("Should find policies containing specific tag")
        void shouldFindByTag() {
            Policy policy = buildPolicy("Tagged Policy",
                    PolicyCategory.ELIGIBILITY, LoanType.HOME_LOAN, PolicyStatus.ACTIVE);
            policy.setTags(List.of("priority", "home-loan", "eligibility"));
            policyRepository.save(policy);

            List<Policy> found = policyRepository.findByTagsContaining("home-loan");

            assertThat(found).hasSize(1);
            assertThat(found.get(0).getTags()).contains("home-loan");
        }
    }
}
