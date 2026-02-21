package com.loanflow.policy.config;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.repository.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for PolicyTemplateInitializer.
 * Tests template initialization logic and template structure.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyTemplateInitializer Tests")
class PolicyTemplateInitializerTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyTemplateInitializer initializer;

    @Nested
    @DisplayName("Template Initialization")
    class TemplateInitialization {

        @Test
        @DisplayName("Should create all 3 templates when none exist")
        void shouldCreateAllTemplatesWhenNoneExist() {
            when(policyRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

            initializer.run(new DefaultApplicationArguments());

            verify(policyRepository, times(3)).save(any(Policy.class));
        }

        @Test
        @DisplayName("Should skip all templates when they already exist")
        void shouldSkipAllTemplatesWhenExist() {
            when(policyRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

            initializer.run(new DefaultApplicationArguments());

            verify(policyRepository, never()).save(any(Policy.class));
        }

        @Test
        @DisplayName("Should create only missing templates")
        void shouldCreateOnlyMissingTemplates() {
            // Personal Loan exists, Home Loan and KCC don't
            when(policyRepository.existsByNameIgnoreCase("Personal Loan - Eligibility Template")).thenReturn(true);
            when(policyRepository.existsByNameIgnoreCase("Home Loan - Eligibility Template")).thenReturn(false);
            when(policyRepository.existsByNameIgnoreCase("KCC - Eligibility Template")).thenReturn(false);
            when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

            initializer.run(new DefaultApplicationArguments());

            verify(policyRepository, times(2)).save(any(Policy.class));
        }
    }

    @Nested
    @DisplayName("Personal Loan Template")
    class PersonalLoanTemplate {

        @Test
        @DisplayName("Should have correct metadata")
        void shouldHaveCorrectMetadata() {
            Policy template = initializer.buildPersonalLoanEligibilityTemplate();

            assertEquals("Personal Loan - Eligibility Template", template.getName());
            assertEquals(PolicyCategory.ELIGIBILITY, template.getCategory());
            assertEquals(LoanType.PERSONAL_LOAN, template.getLoanType());
            assertEquals(PolicyStatus.DRAFT, template.getStatus());
            assertEquals(1, template.getVersionNumber());
            assertTrue(template.getTags().contains("template"));
            assertTrue(template.getTags().contains("personal-loan"));
        }

        @Test
        @DisplayName("Should have 4 rules")
        void shouldHaveFourRules() {
            Policy template = initializer.buildPersonalLoanEligibilityTemplate();

            assertEquals(4, template.getRuleCount());
        }

        @Test
        @DisplayName("Should have salaried approval rule with 4 conditions and 3 actions")
        void shouldHaveSalariedApprovalRule() {
            Policy template = initializer.buildPersonalLoanEligibilityTemplate();

            PolicyRule salariedRule = template.getRules().stream()
                    .filter(r -> r.getName().equals("Salaried Applicant Approval"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(LogicalOperator.AND, salariedRule.getLogicalOperator());
            assertEquals(4, salariedRule.getConditions().size());
            assertEquals(3, salariedRule.getActions().size());
            assertEquals(10, salariedRule.getPriority());

            // Verify APPROVE action is present
            assertTrue(salariedRule.getActions().stream()
                    .anyMatch(a -> a.getType() == ActionType.APPROVE));

            // Verify employmentType IN condition
            assertTrue(salariedRule.getConditions().stream()
                    .anyMatch(c -> "applicant.employmentType".equals(c.getField())
                            && c.getOperator() == ConditionOperator.IN));
        }

        @Test
        @DisplayName("Should have low CIBIL rejection rule with highest priority")
        void shouldHaveLowCibilRejectionRule() {
            Policy template = initializer.buildPersonalLoanEligibilityTemplate();

            PolicyRule rejectionRule = template.getRules().stream()
                    .filter(r -> r.getName().equals("Low CIBIL Rejection"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(5, rejectionRule.getPriority()); // Highest priority (lowest number)
            assertEquals(1, rejectionRule.getConditions().size());
            assertEquals(ConditionOperator.LESS_THAN, rejectionRule.getConditions().get(0).getOperator());
            assertEquals("500", rejectionRule.getConditions().get(0).getValue());
            assertTrue(rejectionRule.getActions().stream()
                    .anyMatch(a -> a.getType() == ActionType.REJECT));
        }
    }

    @Nested
    @DisplayName("Home Loan Template")
    class HomeLoanTemplate {

        @Test
        @DisplayName("Should have correct metadata")
        void shouldHaveCorrectMetadata() {
            Policy template = initializer.buildHomeLoanEligibilityTemplate();

            assertEquals("Home Loan - Eligibility Template", template.getName());
            assertEquals(PolicyCategory.ELIGIBILITY, template.getCategory());
            assertEquals(LoanType.HOME_LOAN, template.getLoanType());
            assertEquals(PolicyStatus.DRAFT, template.getStatus());
            assertTrue(template.getTags().contains("template"));
            assertTrue(template.getTags().contains("home-loan"));
        }

        @Test
        @DisplayName("Should have 4 rules including high-value referral")
        void shouldHaveFourRulesIncludingHighValueReferral() {
            Policy template = initializer.buildHomeLoanEligibilityTemplate();

            assertEquals(4, template.getRuleCount());

            PolicyRule referralRule = template.getRules().stream()
                    .filter(r -> r.getName().equals("High Value Loan Referral"))
                    .findFirst()
                    .orElseThrow();

            assertTrue(referralRule.getActions().stream()
                    .anyMatch(a -> a.getType() == ActionType.REFER));
            assertTrue(referralRule.getActions().stream()
                    .anyMatch(a -> a.getType() == ActionType.ASSIGN_TO_ROLE));
        }

        @Test
        @DisplayName("Should require property documents on standard approval")
        void shouldRequirePropertyDocuments() {
            Policy template = initializer.buildHomeLoanEligibilityTemplate();

            PolicyRule approvalRule = template.getRules().stream()
                    .filter(r -> r.getName().equals("Standard Home Loan Approval"))
                    .findFirst()
                    .orElseThrow();

            // Verify REQUIRE_DOCUMENT action with PROPERTY_PAPERS
            assertTrue(approvalRule.getActions().stream()
                    .anyMatch(a -> a.getType() == ActionType.REQUIRE_DOCUMENT
                            && "PROPERTY_PAPERS".equals(a.getParameters().get("documentType"))));

            // Verify property.estimatedValue IS_NOT_NULL condition
            assertTrue(approvalRule.getConditions().stream()
                    .anyMatch(c -> "property.estimatedValue".equals(c.getField())
                            && c.getOperator() == ConditionOperator.IS_NOT_NULL));
        }
    }

    @Nested
    @DisplayName("KCC Template")
    class KccTemplate {

        @Test
        @DisplayName("Should have correct metadata for KCC loan type")
        void shouldHaveCorrectMetadata() {
            Policy template = initializer.buildKccEligibilityTemplate();

            assertEquals("KCC - Eligibility Template", template.getName());
            assertEquals(PolicyCategory.ELIGIBILITY, template.getCategory());
            assertEquals(LoanType.KCC, template.getLoanType());
            assertEquals(PolicyStatus.DRAFT, template.getStatus());
            assertTrue(template.getTags().contains("template"));
            assertTrue(template.getTags().contains("kcc"));
            assertTrue(template.getTags().contains("agriculture"));
        }

        @Test
        @DisplayName("Should have 3 rules")
        void shouldHaveThreeRules() {
            Policy template = initializer.buildKccEligibilityTemplate();

            assertEquals(3, template.getRuleCount());
        }

        @Test
        @DisplayName("Should have agriculture-specific conditions")
        void shouldHaveAgricultureConditions() {
            Policy template = initializer.buildKccEligibilityTemplate();

            // Verify land ownership condition exists
            assertTrue(template.getRules().stream()
                    .flatMap(r -> r.getConditions().stream())
                    .anyMatch(c -> "applicant.landOwnership".equals(c.getField())));

            // Verify crop type condition exists
            assertTrue(template.getRules().stream()
                    .flatMap(r -> r.getConditions().stream())
                    .anyMatch(c -> "applicant.cropType".equals(c.getField())));

            // Verify land area condition for large farmers
            assertTrue(template.getRules().stream()
                    .flatMap(r -> r.getConditions().stream())
                    .anyMatch(c -> "applicant.landArea".equals(c.getField())
                            && c.getOperator() == ConditionOperator.GREATER_THAN));

            // Verify subsidized interest rate (4.0%)
            PolicyRule standardRule = template.getRules().stream()
                    .filter(r -> r.getName().equals("Standard KCC Approval"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(standardRule.getActions().stream()
                    .anyMatch(a -> a.getType() == ActionType.SET_INTEREST_RATE
                            && "4.0".equals(a.getParameters().get("rate"))));
        }
    }
}
