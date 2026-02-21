package com.loanflow.loan.decision;

import com.loanflow.loan.decision.mapper.DecisionFactMapper;
import com.loanflow.loan.decision.mapper.DecisionFactMapper.DecisionFacts;
import com.loanflow.loan.decision.model.*;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Decision Fact Mapper")
class DecisionFactMapperTest {

    private DecisionFactMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DecisionFactMapper();
    }

    @Nested
    @DisplayName("Entity to Facts Mapping")
    class EntityToFactsTests {

        @Test
        @DisplayName("Should map LoanApplication entity to all fact objects")
        void shouldMapEntityToFacts() {
            LoanApplication application = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2026-000001")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("500000"))
                    .tenureMonths(36)
                    .status(LoanStatus.DOCUMENT_VERIFICATION)
                    .customerId(UUID.randomUUID())
                    .customerEmail("test@example.com")
                    .build();

            DecisionFacts facts = mapper.mapToFacts(application);

            assertThat(facts.loanApplication()).isNotNull();
            assertThat(facts.applicant()).isNotNull();
            assertThat(facts.employmentDetails()).isNotNull();
            assertThat(facts.creditReport()).isNotNull();
            assertThat(facts.eligibilityResult()).isNotNull();
            assertThat(facts.pricingResult()).isNotNull();
            assertThat(facts.collateral()).isNull(); // No collateral by default
        }

        @Test
        @DisplayName("Should map product code from LoanType correctly")
        void shouldMapProductCode() {
            LoanApplication application = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2026-000002")
                    .loanType(LoanType.HOME_LOAN)
                    .requestedAmount(new BigDecimal("7500000"))
                    .tenureMonths(240)
                    .status(LoanStatus.CREDIT_CHECK)
                    .customerId(UUID.randomUUID())
                    .customerEmail("test@example.com")
                    .build();

            DecisionFacts facts = mapper.mapToFacts(application);

            assertThat(facts.loanApplication().getProductCode()).isEqualTo("HL");
            assertThat(facts.loanApplication().getRequestedAmount()).isEqualTo(7500000);
            assertThat(facts.loanApplication().getTenureMonths()).isEqualTo(240);
        }

        @Test
        @DisplayName("Should preserve existing CIBIL score from application")
        void shouldPreserveExistingCibilScore() {
            LoanApplication application = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2026-000003")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("300000"))
                    .tenureMonths(24)
                    .cibilScore(780)
                    .status(LoanStatus.CREDIT_CHECK)
                    .customerId(UUID.randomUUID())
                    .customerEmail("test@example.com")
                    .build();

            DecisionFacts facts = mapper.mapToFacts(application);

            assertThat(facts.creditReport().getCreditScore()).isEqualTo(780);
        }

        @Test
        @DisplayName("Should use default CIBIL score when null")
        void shouldUseDefaultCibilScoreWhenNull() {
            LoanApplication application = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2026-000004")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("300000"))
                    .tenureMonths(24)
                    .cibilScore(null)
                    .status(LoanStatus.CREDIT_CHECK)
                    .customerId(UUID.randomUUID())
                    .customerEmail("test@example.com")
                    .build();

            DecisionFacts facts = mapper.mapToFacts(application);

            assertThat(facts.creditReport().getCreditScore()).isEqualTo(700);
        }

        @Test
        @DisplayName("Should set default applicant as PRIMARY with sensible defaults")
        void shouldSetDefaultApplicant() {
            LoanApplication application = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2026-000005")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("500000"))
                    .tenureMonths(36)
                    .status(LoanStatus.CREDIT_CHECK)
                    .customerId(UUID.randomUUID())
                    .customerEmail("test@example.com")
                    .build();

            DecisionFacts facts = mapper.mapToFacts(application);

            assertThat(facts.applicant().getApplicantType()).isEqualTo("PRIMARY");
            assertThat(facts.applicant().getAge()).isGreaterThan(20);
            assertThat(facts.applicant().getPan()).isNotNull();
            assertThat(facts.applicant().isPanVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("Loan Type to Product Code Mapping")
    class ProductCodeMappingTests {

        @Test
        @DisplayName("Should map all loan types to correct product codes")
        void shouldMapAllLoanTypes() {
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.PERSONAL_LOAN)).isEqualTo("PL");
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.HOME_LOAN)).isEqualTo("HL");
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.VEHICLE_LOAN)).isEqualTo("VL");
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.GOLD_LOAN)).isEqualTo("GL");
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.EDUCATION_LOAN)).isEqualTo("EL");
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.BUSINESS_LOAN)).isEqualTo("BL");
            assertThat(mapper.mapLoanTypeToProductCode(LoanType.LAP)).isEqualTo("LAP");
        }

        @Test
        @DisplayName("Should default to PL when loan type is null")
        void shouldDefaultToPLWhenNull() {
            assertThat(mapper.mapLoanTypeToProductCode(null)).isEqualTo("PL");
        }
    }

    @Nested
    @DisplayName("Ad-hoc Facts Mapping")
    class AdHocFactsTests {

        @Test
        @DisplayName("Should create facts from explicit parameters")
        void shouldCreateFromExplicitParams() {
            String appId = UUID.randomUUID().toString();
            String applicantId = UUID.randomUUID().toString();

            LoanApplicationFact appFact = LoanApplicationFact.builder()
                    .id(appId).productCode("PL").requestedAmount(500000).tenureMonths(36).build();
            ApplicantFact applicantFact = ApplicantFact.builder()
                    .id(applicantId).applicationId(appId).applicantType("PRIMARY")
                    .age(30).pan("ABCDE1234F").panVerified(true).build();
            EmploymentDetailsFact empFact = EmploymentDetailsFact.builder()
                    .applicantId(applicantId).employmentType(EmploymentType.SALARIED)
                    .netMonthlyIncome(50000).build();
            CreditReportFact creditFact = CreditReportFact.builder()
                    .applicantId(applicantId).creditScore(750).build();

            DecisionFacts facts = mapper.mapToFacts(appFact, applicantFact, empFact, creditFact, null);

            assertThat(facts.loanApplication()).isEqualTo(appFact);
            assertThat(facts.applicant()).isEqualTo(applicantFact);
            assertThat(facts.eligibilityResult()).isNotNull();
            assertThat(facts.eligibilityResult().getApplicationId()).isEqualTo(appId);
            assertThat(facts.pricingResult()).isNotNull();
            assertThat(facts.pricingResult().getApplicationId()).isEqualTo(appId);
        }
    }
}
