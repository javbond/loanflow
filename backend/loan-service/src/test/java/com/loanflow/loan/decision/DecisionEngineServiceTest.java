package com.loanflow.loan.decision;

import com.loanflow.loan.decision.config.DroolsConfig;
import com.loanflow.loan.decision.mapper.DecisionFactMapper;
import com.loanflow.loan.decision.mapper.DecisionFactMapper.DecisionFacts;
import com.loanflow.loan.decision.model.*;
import com.loanflow.loan.decision.service.ConfigService;
import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.decision.service.RbiRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for the Drools Decision Engine.
 * Tests rule execution against various loan scenarios.
 *
 * These tests create a real KieContainer (not mocked) to verify
 * that DRL rules compile and execute correctly.
 */
@DisplayName("Decision Engine Service — Drools Rule Evaluation")
class DecisionEngineServiceTest {

    private DecisionEngineService decisionEngineService;
    private DecisionFactMapper factMapper;
    private KieContainer kieContainer;

    @BeforeEach
    void setUp() {
        // Use real DroolsConfig to build KieContainer from DRL files
        DroolsConfig droolsConfig = new DroolsConfig();
        kieContainer = droolsConfig.kieContainer();

        ConfigService configService = new ConfigService();
        RbiRateService rbiRateService = new RbiRateService();
        factMapper = new DecisionFactMapper();

        decisionEngineService = new DecisionEngineService(
                kieContainer, configService, rbiRateService, factMapper);
    }

    // =========================================================================
    // DRL COMPILATION
    // =========================================================================

    @Nested
    @DisplayName("DRL Compilation")
    class DrlCompilationTests {

        @Test
        @DisplayName("Should compile all DRL files without errors")
        void shouldCompileDrlFilesWithoutErrors() {
            assertThat(kieContainer).isNotNull();
            assertThat(kieContainer.getKieBase()).isNotNull();
        }
    }

    // =========================================================================
    // ELIGIBILITY — HAPPY PATH
    // =========================================================================

    @Nested
    @DisplayName("Eligibility — Eligible Scenarios")
    class EligibilityEligibleTests {

        @Test
        @DisplayName("Should approve eligible personal loan — CIBIL 750, income 50K, age 30")
        void shouldApproveEligiblePersonalLoan() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 750, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PL-001");

            assertThat(result.eligible()).isTrue();
            assertThat(result.eligibilityStatus()).isEqualTo("ELIGIBLE");
            assertThat(result.rejectionReasons()).isEmpty();
            assertThat(result.rulesFired()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should approve eligible home loan — CIBIL 700, income 80K, age 35")
        void shouldApproveEligibleHomeLoan() {
            DecisionFacts facts = buildHomeLoanFacts(7500000, 240, 700, 35, 80000, 10000000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-HL-001");

            assertThat(result.eligible()).isTrue();
            assertThat(result.eligibilityStatus()).isEqualTo("ELIGIBLE");
        }
    }

    // =========================================================================
    // ELIGIBILITY — REJECTION SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Eligibility — Rejection Scenarios")
    class EligibilityRejectionTests {

        @Test
        @DisplayName("Should reject applicant with low CIBIL score (< 550)")
        void shouldRejectLowCibilScore() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 500, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-001");

            assertThat(result.eligible()).isFalse();
            assertThat(result.eligibilityStatus()).isEqualTo("REJECTED");
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("Credit score"));
        }

        @Test
        @DisplayName("Should reject underage applicant (age < 21)")
        void shouldRejectUnderageApplicant() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 700, 18, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-002");

            assertThat(result.eligible()).isFalse();
            assertThat(result.eligibilityStatus()).isEqualTo("REJECTED");
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("21 years"));
        }

        @Test
        @DisplayName("Should reject when age at maturity exceeds 65")
        void shouldRejectOverageApplicant() {
            DecisionFacts facts = buildFacts("PL", 500000, 240, 700, 55, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-003");

            assertThat(result.eligible()).isFalse();
            assertThat(result.eligibilityStatus()).isEqualTo("REJECTED");
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("maturity"));
        }

        @Test
        @DisplayName("Should reject PL above max amount (50 Lakhs)")
        void shouldRejectExcessiveAmount() {
            DecisionFacts facts = buildFacts("PL", 6000000, 36, 750, 30, 100000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-004");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("50 Lakhs"));
        }

        @Test
        @DisplayName("Should reject applicant with low income for PL")
        void shouldRejectLowIncome() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 700, 30, 20000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-005");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("income"));
        }

        @Test
        @DisplayName("Should reject applicant with 90+ DPD")
        void shouldRejectDpd90Plus() {
            DecisionFacts facts = buildFactsWithCreditIssues("PL", 500000, 36, 700, 30, 50000, 1, 0);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-006");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("90+"));
        }

        @Test
        @DisplayName("Should reject applicant with written-off accounts")
        void shouldRejectWrittenOffAccounts() {
            DecisionFacts facts = buildFactsWithCreditIssues("PL", 500000, 36, 700, 30, 50000, 0, 1);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REJECT-007");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("written-off"));
        }

        @Test
        @DisplayName("Should reject PAN-less applicant")
        void shouldRejectNoPan() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-PAN-001")
                    .productCode("PL").requestedAmount(500000).tenureMonths(36).build();
            ApplicantFact applicantFact = ApplicantFact.builder()
                    .id(applicantId).applicationId(appId)
                    .applicantType("PRIMARY").age(30).gender("MALE")
                    .pan(null).panVerified(false).build();
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = buildDefaultCredit(applicantId, 700);

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PAN-001");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("PAN"));
        }
    }

    // =========================================================================
    // ELIGIBILITY — REFER SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Eligibility — Refer Scenarios")
    class EligibilityReferTests {

        @Test
        @DisplayName("Should refer marginal CIBIL score (550-650)")
        void shouldReferMarginalCibilScore() {
            DecisionFacts facts = buildFacts("PL", 300000, 36, 600, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REFER-001");

            assertThat(result.eligibilityStatus()).isEqualTo("REFER");
            assertThat(result.decision()).isEqualTo("REFERRED");
            assertThat(result.referReason()).contains("550-650");
        }

        @Test
        @DisplayName("Should refer high enquiry count (> 5)")
        void shouldReferHighEnquiryCount() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-REFER-002")
                    .productCode("PL").requestedAmount(300000).tenureMonths(36).build();
            ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, 30);
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = CreditReportFact.builder()
                    .id(UUID.randomUUID().toString()).applicantId(applicantId)
                    .creditScore(700).dpd90PlusCount(0).writtenOffAccounts(0)
                    .enquiryCount30Days(8).build();

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-REFER-002");

            assertThat(result.eligibilityStatus()).isEqualTo("REFER");
            assertThat(result.referReason()).contains("enquiry");
        }
    }

    // =========================================================================
    // PRICING RULES
    // =========================================================================

    @Nested
    @DisplayName("Pricing — Base Rate & Adjustments")
    class PricingTests {

        @Test
        @DisplayName("Should calculate correct base rate for PL (repo + 3.50)")
        void shouldCalculateCorrectBaseRate() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 750, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-001");

            // Repo rate 6.50 + PL spread 3.50 = 10.00 base
            assertThat(result.baseRate()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should apply credit score discount for excellent score (750+)")
        void shouldApplyCibilScoreDiscount() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 800, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-002");

            // Base 10.0, CIBIL excellent discount -0.50
            assertThat(result.totalDiscounts()).isLessThan(0);
            assertThat(result.riskTier()).isEqualTo("A");
        }

        @Test
        @DisplayName("Should apply women borrower discount")
        void shouldApplyWomenBorrowerDiscount() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-PRICE-003")
                    .productCode("PL").requestedAmount(500000).tenureMonths(36).build();
            ApplicantFact applicantFact = ApplicantFact.builder()
                    .id(applicantId).applicationId(appId)
                    .applicantType("PRIMARY").age(30).gender("FEMALE")
                    .pan("ABCDE1234F").panVerified(true).build();
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = buildDefaultCredit(applicantId, 750);

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-003");

            // Should have WOMEN_BORROWER discount (-0.05)
            assertThat(result.totalDiscounts()).isLessThan(0);
            // Total discounts should be more negative than just credit score discount
            assertThat(result.interestRate()).isLessThan(result.baseRate());
        }

        @Test
        @DisplayName("Should apply government employer discount")
        void shouldApplyGovernmentEmployerDiscount() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-PRICE-004")
                    .productCode("PL").requestedAmount(500000).tenureMonths(36).build();
            ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, 30);
            EmploymentDetailsFact empFact = EmploymentDetailsFact.builder()
                    .id(UUID.randomUUID().toString()).applicantId(applicantId)
                    .employmentType(EmploymentType.SALARIED)
                    .employerCategory(EmployerCategory.GOVERNMENT)
                    .netMonthlyIncome(50000).totalExperienceYears(5).yearsInCurrentJob(2).build();
            CreditReportFact creditFact = buildDefaultCredit(applicantId, 750);

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-004");

            // Government discount (-0.50) + credit score discount (-0.50)
            assertThat(result.totalDiscounts()).isLessThanOrEqualTo(-1.0);
        }

        @Test
        @DisplayName("Should calculate processing fee for PL (2%, min 2000, max 25000)")
        void shouldCalculateProcessingFee() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 750, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-005");

            // 500000 * 2% = 10000
            assertThat(result.processingFee()).isEqualTo(10000.0);
        }

        @Test
        @DisplayName("Should calculate HL base rate (repo + 2.50)")
        void shouldCalculateHomeLoanBaseRate() {
            DecisionFacts facts = buildHomeLoanFacts(7500000, 240, 750, 35, 80000, 10000000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-006");

            // Repo rate 6.50 + HL spread 2.50 = 9.00 base
            assertThat(result.baseRate()).isEqualTo(9.0);
        }

        @Test
        @DisplayName("Should apply short tenure discount for PL <= 24 months")
        void shouldApplyShortTenureDiscount() {
            DecisionFacts facts = buildFacts("PL", 300000, 12, 750, 30, 80000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-007");

            // Should have SHORT_TENURE discount (-0.25)
            assertThat(result.totalDiscounts()).isLessThan(0);
        }

        @Test
        @DisplayName("Should apply credit score premium for fair score (650-699)")
        void shouldApplyCreditScorePremium() {
            DecisionFacts facts = buildFacts("PL", 300000, 36, 670, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-008");

            // Fair score premium +0.50
            assertThat(result.totalPremiums()).isGreaterThan(0);
            assertThat(result.riskTier()).isEqualTo("C");
        }

        @Test
        @DisplayName("Should calculate final interest rate with floor and cap")
        void shouldCalculateFinalRate() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 750, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-PRICE-009");

            assertThat(result.interestRate()).isGreaterThan(0);
            assertThat(result.interestRate()).isGreaterThanOrEqualTo(7.0);
            assertThat(result.interestRate()).isLessThanOrEqualTo(24.0);
        }
    }

    // =========================================================================
    // PRODUCT-SPECIFIC TESTS
    // =========================================================================

    @Nested
    @DisplayName("Product-Specific Rules")
    class ProductSpecificTests {

        @Test
        @DisplayName("Should handle home loan eligibility with LTV check")
        void shouldHandleHomeLoanEligibility() {
            // LTV = 9M / 10M = 90% > 80% → should reject
            DecisionFacts facts = buildHomeLoanFacts(9000000, 240, 750, 35, 80000, 10000000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-HL-LTV-001");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("LTV") || r.contains("80%"));
        }

        @Test
        @DisplayName("Should handle gold loan eligibility")
        void shouldHandleGoldLoanEligibility() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-GL-001")
                    .productCode("GL").requestedAmount(500000).tenureMonths(12)
                    .goldValue(800000).goldWeightGrams(50).build();
            ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, 40);
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = buildDefaultCredit(applicantId, 700);

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-GL-001");

            // LTV = 500K / 800K = 62.5% < 75% → eligible
            assertThat(result.eligible()).isTrue();
        }

        @Test
        @DisplayName("Should reject gold loan with excessive LTV")
        void shouldRejectGoldLoanHighLtv() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-GL-002")
                    .productCode("GL").requestedAmount(700000).tenureMonths(12)
                    .goldValue(800000).goldWeightGrams(50).build();
            ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, 40);
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = buildDefaultCredit(applicantId, 700);

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-GL-002");

            // LTV = 700K / 800K = 87.5% > 75% → reject
            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("75%"));
        }

        @Test
        @DisplayName("Should reject gold loan with insufficient weight (< 10g)")
        void shouldRejectLowGoldWeight() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-GL-003")
                    .productCode("GL").requestedAmount(50000).tenureMonths(12)
                    .goldValue(100000).goldWeightGrams(5).build();
            ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, 40);
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = buildDefaultCredit(applicantId, 700);

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-GL-003");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("10 grams"));
        }
    }

    // =========================================================================
    // RISK CATEGORY
    // =========================================================================

    @Nested
    @DisplayName("Risk Category Determination")
    class RiskCategoryTests {

        @Test
        @DisplayName("Should set LOW risk for tier A (CIBIL 750+)")
        void shouldSetLowRiskForTierA() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 800, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-RISK-001");

            assertThat(result.riskTier()).isEqualTo("A");
            assertThat(result.riskCategory()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("Should set MEDIUM risk for tier B (CIBIL 700-749)")
        void shouldSetMediumRiskForTierB() {
            DecisionFacts facts = buildFacts("PL", 300000, 36, 720, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-RISK-002");

            assertThat(result.riskTier()).isEqualTo("B");
            assertThat(result.riskCategory()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Should set HIGH risk for rejected applicants")
        void shouldSetHighRiskForRejected() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 500, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-RISK-003");

            assertThat(result.riskCategory()).isEqualTo("HIGH");
        }
    }

    // =========================================================================
    // MULTIPLE RULES FIRING
    // =========================================================================

    @Nested
    @DisplayName("Multiple Rules Firing")
    class MultipleRulesTests {

        @Test
        @DisplayName("Should fire multiple rules in sequence (eligibility + pricing)")
        void shouldFireMultipleRulesInSequence() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 750, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-MULTI-001");

            // At minimum: eligibility check + base rate + credit score + processing fee + final rate
            assertThat(result.rulesFired()).isGreaterThan(3);
        }

        @Test
        @DisplayName("Should handle null collateral (skip collateral rules)")
        void shouldHandleNullCollateral() {
            DecisionFacts facts = buildFacts("PL", 500000, 36, 750, 30, 50000);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-MULTI-002");

            // Should still work without collateral
            assertThat(result.rulesFired()).isGreaterThan(0);
            assertThat(result.interestRate()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should accumulate multiple rejection reasons")
        void shouldAccumulateRejectionReasons() {
            // Age 18 + CIBIL 500 + DPD 90+ → multiple rejections
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).applicationNumber("TEST-MULTI-003")
                    .productCode("PL").requestedAmount(500000).tenureMonths(36).build();
            ApplicantFact applicantFact = ApplicantFact.builder()
                    .id(applicantId).applicationId(appId)
                    .applicantType("PRIMARY").age(18).gender("MALE")
                    .pan("ABCDE1234F").panVerified(true).build();
            EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, 50000);
            CreditReportFact creditFact = CreditReportFact.builder()
                    .id(UUID.randomUUID().toString()).applicantId(applicantId)
                    .creditScore(500).dpd90PlusCount(2).writtenOffAccounts(1)
                    .enquiryCount30Days(0).build();

            DecisionFacts facts = factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-MULTI-003");

            assertThat(result.eligible()).isFalse();
            assertThat(result.rejectionReasons().size()).isGreaterThan(1);
        }
    }

    // =========================================================================
    // INCOME VERIFICATION RULES (US-017)
    // =========================================================================

    @Nested
    @DisplayName("Income Verification Rules")
    class IncomeVerificationTests {

        @Test
        @DisplayName("Should reject when DTI ratio exceeds 50%")
        void shouldRejectHighDtiRatio() {
            DecisionFacts facts = buildFactsWithIncome("PL", 500000, 36, 750, 30, 50000,
                    true, 50000, 0.55, 90, 3);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-INCOME-001");

            assertThat(result.eligible()).isFalse();
            assertThat(result.eligibilityStatus()).isEqualTo("REJECTED");
            assertThat(result.rejectionReasons()).anyMatch(r -> r.contains("DTI") || r.contains("Debt-to-Income"));
        }

        @Test
        @DisplayName("Should refer when income consistency below 70%")
        void shouldReferIncomeMismatch() {
            DecisionFacts facts = buildFactsWithIncome("PL", 300000, 36, 750, 30, 50000,
                    true, 50000, 0.30, 60, 1);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-INCOME-002");

            assertThat(result.eligibilityStatus()).isEqualTo("REFER");
            assertThat(result.referReason()).containsIgnoringCase("income");
        }

        @Test
        @DisplayName("Should refer when cheque bounce count exceeds 3")
        void shouldReferHighBounceCount() {
            DecisionFacts facts = buildFactsWithIncome("PL", 300000, 36, 750, 30, 50000,
                    true, 50000, 0.30, 90, 5);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-INCOME-003");

            assertThat(result.eligibilityStatus()).isEqualTo("REFER");
            assertThat(result.referReason()).containsIgnoringCase("bounce");
        }

        @Test
        @DisplayName("Should apply verified income discount for consistency >= 90%")
        void shouldApplyVerifiedIncomeDiscount() {
            DecisionFacts facts = buildFactsWithIncome("PL", 300000, 36, 750, 30, 50000,
                    true, 50000, 0.25, 95, 0);
            DecisionResult result = decisionEngineService.evaluateWithFacts(facts, "TEST-INCOME-004");

            // Should have VERIFIED_INCOME_GOOD discount (-0.15)
            assertThat(result.eligible()).isTrue();
            assertThat(result.totalDiscounts()).isLessThan(0);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private DecisionFacts buildFacts(String productCode, double amount, int tenure,
                                     int cibilScore, int age, double income) {
        String appId = UUID.randomUUID().toString();
        String applicantId = UUID.randomUUID().toString();

        LoanApplicationFact appFact = LoanApplicationFact.builder()
                .id(appId).applicationNumber("TEST-" + productCode)
                .productCode(productCode).requestedAmount(amount).tenureMonths(tenure).build();
        ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, age);
        EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, income);
        CreditReportFact creditFact = buildDefaultCredit(applicantId, cibilScore);

        return factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
    }

    private DecisionFacts buildFactsWithCreditIssues(String productCode, double amount, int tenure,
                                                      int cibilScore, int age, double income,
                                                      int dpd90Plus, int writtenOff) {
        String appId = UUID.randomUUID().toString();
        String applicantId = UUID.randomUUID().toString();

        LoanApplicationFact appFact = LoanApplicationFact.builder()
                .id(appId).applicationNumber("TEST-" + productCode)
                .productCode(productCode).requestedAmount(amount).tenureMonths(tenure).build();
        ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, age);
        EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, income);
        CreditReportFact creditFact = CreditReportFact.builder()
                .id(UUID.randomUUID().toString()).applicantId(applicantId)
                .creditScore(cibilScore).dpd90PlusCount(dpd90Plus)
                .writtenOffAccounts(writtenOff).enquiryCount30Days(1).build();

        return factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);
    }

    private DecisionFacts buildHomeLoanFacts(double amount, int tenure, int cibilScore,
                                             int age, double income, double propertyValue) {
        String appId = UUID.randomUUID().toString();
        String applicantId = UUID.randomUUID().toString();

        LoanApplicationFact appFact = LoanApplicationFact.builder()
                .id(appId).applicationNumber("TEST-HL")
                .productCode("HL").requestedAmount(amount).tenureMonths(tenure)
                .propertyValue(propertyValue).build();
        ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, age);
        EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, income);
        CreditReportFact creditFact = buildDefaultCredit(applicantId, cibilScore);

        CollateralFact collateralFact = CollateralFact.builder()
                .id(UUID.randomUUID().toString()).applicationId(appId)
                .marketValue(propertyValue).collateralType("PROPERTY").build();

        return factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, collateralFact);
    }

    private ApplicantFact buildDefaultApplicant(String applicantId, String appId, int age) {
        return ApplicantFact.builder()
                .id(applicantId).applicationId(appId)
                .applicantType("PRIMARY").age(age).gender("MALE")
                .pan("ABCDE1234F").panVerified(true)
                .politicallyExposed(false).existingEmi(0)
                .hasSalaryAccount(false).existingCustomer(false).existingLoanDpd(0)
                .build();
    }

    private EmploymentDetailsFact buildDefaultEmployment(String applicantId, double income) {
        return EmploymentDetailsFact.builder()
                .id(UUID.randomUUID().toString()).applicantId(applicantId)
                .employmentType(EmploymentType.SALARIED).employerCategory(EmployerCategory.PRIVATE)
                .netMonthlyIncome(income).totalExperienceYears(5).yearsInCurrentJob(2)
                .build();
    }

    private CreditReportFact buildDefaultCredit(String applicantId, int cibilScore) {
        return CreditReportFact.builder()
                .id(UUID.randomUUID().toString()).applicantId(applicantId)
                .creditScore(cibilScore).dpd90PlusCount(0).writtenOffAccounts(0)
                .enquiryCount30Days(1)
                .build();
    }

    private DecisionFacts buildFactsWithIncome(String productCode, double amount, int tenure,
                                                int cibilScore, int age, double income,
                                                boolean incomeVerified, double verifiedIncome,
                                                double dtiRatio, int consistencyScore, int bounceCount) {
        String appId = UUID.randomUUID().toString();
        String applicantId = UUID.randomUUID().toString();

        LoanApplicationFact appFact = LoanApplicationFact.builder()
                .id(appId).applicationNumber("TEST-" + productCode)
                .productCode(productCode).requestedAmount(amount).tenureMonths(tenure).build();
        ApplicantFact applicantFact = buildDefaultApplicant(applicantId, appId, age);
        EmploymentDetailsFact empFact = buildDefaultEmployment(applicantId, income);
        CreditReportFact creditFact = buildDefaultCredit(applicantId, cibilScore);

        IncomeVerificationFact incomeFact = IncomeVerificationFact.builder()
                .applicationId(appId)
                .incomeVerified(incomeVerified)
                .verifiedMonthlyIncome(verifiedIncome)
                .dtiRatio(dtiRatio)
                .incomeConsistencyScore(consistencyScore)
                .annualItrIncome(verifiedIncome * 12)
                .annualGstTurnover(0)
                .gstFilingCount12Months(0)
                .avgMonthlyBankBalance(verifiedIncome * 2)
                .avgMonthlySalaryCredits(verifiedIncome)
                .chequeBounceCount(bounceCount)
                .build();

        EligibilityResultFact eligibilityResult = EligibilityResultFact.builder()
                .applicationId(appId).build();
        PricingResultFact pricingResult = PricingResultFact.builder()
                .applicationId(appId).loanAmount(amount).tenureMonths(tenure).build();

        return new DecisionFacts(appFact, applicantFact, empFact, creditFact,
                eligibilityResult, pricingResult, null, incomeFact);
    }
}
