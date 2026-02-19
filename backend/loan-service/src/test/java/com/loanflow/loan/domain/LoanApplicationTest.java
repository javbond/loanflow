package com.loanflow.loan.domain;

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

/**
 * TDD Test Cases for LoanApplication Entity
 * Tests written FIRST, then implementation follows
 */
@DisplayName("LoanApplication Entity Tests")
class LoanApplicationTest {

    private LoanApplication loanApplication;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        loanApplication = LoanApplication.builder()
                .customerId(customerId)
                .loanType(LoanType.HOME_LOAN)
                .requestedAmount(new BigDecimal("5000000"))
                .tenureMonths(240)
                .purpose("Purchase of residential property")
                .build();
    }

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create loan application with DRAFT status")
        void shouldCreateWithDraftStatus() {
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.DRAFT);
        }

        @Test
        @DisplayName("Should generate application number on creation")
        void shouldGenerateApplicationNumber() {
            loanApplication.generateApplicationNumber();
            assertThat(loanApplication.getApplicationNumber())
                    .isNotNull()
                    .startsWith("LN-");
        }

        @Test
        @DisplayName("Should set default values on creation")
        void shouldSetDefaultValues() {
            assertThat(loanApplication.getVersion()).isNull(); // JPA handles this
            assertThat(loanApplication.getApprovedAmount()).isNull();
            assertThat(loanApplication.getInterestRate()).isNull();
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should allow DRAFT to SUBMITTED transition")
        void shouldAllowDraftToSubmitted() {
            assertThatCode(() -> loanApplication.submit())
                    .doesNotThrowAnyException();
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.SUBMITTED);
        }

        @Test
        @DisplayName("Should not allow invalid status transition")
        void shouldNotAllowInvalidTransition() {
            // DRAFT cannot go directly to APPROVED
            assertThatThrownBy(() -> loanApplication.transitionTo(LoanStatus.APPROVED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("Should track status change timestamp")
        void shouldTrackStatusChangeTimestamp() {
            loanApplication.submit();
            assertThat(loanApplication.getSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should allow full workflow: DRAFT -> SUBMITTED -> ... -> APPROVED")
        void shouldAllowFullWorkflow() {
            // DRAFT -> SUBMITTED
            loanApplication.submit();
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.SUBMITTED);

            // SUBMITTED -> DOCUMENT_VERIFICATION
            loanApplication.transitionTo(LoanStatus.DOCUMENT_VERIFICATION);
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.DOCUMENT_VERIFICATION);

            // DOCUMENT_VERIFICATION -> CREDIT_CHECK
            loanApplication.transitionTo(LoanStatus.CREDIT_CHECK);
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.CREDIT_CHECK);

            // CREDIT_CHECK -> UNDERWRITING
            loanApplication.transitionTo(LoanStatus.UNDERWRITING);
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.UNDERWRITING);

            // UNDERWRITING -> APPROVED
            loanApplication.approve(new BigDecimal("4500000"), new BigDecimal("8.75"));
            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("Approval Tests")
    class ApprovalTests {

        @BeforeEach
        void moveToUnderwriting() {
            loanApplication.submit();
            loanApplication.transitionTo(LoanStatus.DOCUMENT_VERIFICATION);
            loanApplication.transitionTo(LoanStatus.CREDIT_CHECK);
            loanApplication.transitionTo(LoanStatus.UNDERWRITING);
        }

        @Test
        @DisplayName("Should set approved amount and interest rate on approval")
        void shouldSetApprovalDetails() {
            BigDecimal approvedAmount = new BigDecimal("4500000");
            BigDecimal interestRate = new BigDecimal("8.75");

            loanApplication.approve(approvedAmount, interestRate);

            assertThat(loanApplication.getApprovedAmount()).isEqualTo(approvedAmount);
            assertThat(loanApplication.getInterestRate()).isEqualTo(interestRate);
            assertThat(loanApplication.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate EMI on approval")
        void shouldCalculateEmi() {
            loanApplication.approve(new BigDecimal("5000000"), new BigDecimal("8.5"));

            assertThat(loanApplication.getEmiAmount())
                    .isNotNull()
                    .isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should not approve more than requested amount")
        void shouldNotApproveMoreThanRequested() {
            BigDecimal moreThanRequested = loanApplication.getRequestedAmount().add(BigDecimal.ONE);

            assertThatThrownBy(() -> loanApplication.approve(moreThanRequested, new BigDecimal("8.5")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot exceed requested amount");
        }
    }

    @Nested
    @DisplayName("Rejection Tests")
    class RejectionTests {

        @Test
        @DisplayName("Should set rejection reason when rejected")
        void shouldSetRejectionReason() {
            loanApplication.submit();
            loanApplication.transitionTo(LoanStatus.DOCUMENT_VERIFICATION);
            loanApplication.transitionTo(LoanStatus.CREDIT_CHECK);
            loanApplication.transitionTo(LoanStatus.UNDERWRITING);

            loanApplication.reject("CIBIL score below threshold");

            assertThat(loanApplication.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(loanApplication.getRejectionReason()).isEqualTo("CIBIL score below threshold");
            assertThat(loanApplication.getRejectedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("EMI Calculation Tests")
    class EmiCalculationTests {

        @Test
        @DisplayName("Should calculate correct EMI for home loan")
        void shouldCalculateCorrectEmi() {
            // P = 50,00,000, R = 8.5% p.a. (0.708333% monthly), N = 240 months
            // EMI = P * R * (1+R)^N / ((1+R)^N - 1)
            // Expected EMI ~ 43,391

            BigDecimal principal = new BigDecimal("5000000");
            BigDecimal annualRate = new BigDecimal("8.5");
            int tenureMonths = 240;

            BigDecimal emi = LoanApplication.calculateEmi(principal, annualRate, tenureMonths);

            assertThat(emi)
                    .isGreaterThan(new BigDecimal("43000"))
                    .isLessThan(new BigDecimal("44000"));
        }

        @Test
        @DisplayName("Should return zero EMI for zero principal")
        void shouldReturnZeroForZeroPrincipal() {
            BigDecimal emi = LoanApplication.calculateEmi(BigDecimal.ZERO, new BigDecimal("8.5"), 240);
            assertThat(emi).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate minimum loan amount")
        void shouldValidateMinimumAmount() {
            assertThatThrownBy(() -> LoanApplication.builder()
                    .customerId(customerId)
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("5000")) // Below minimum
                    .tenureMonths(24)
                    .purpose("Test")
                    .build()
                    .validate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Minimum loan amount");
        }

        @Test
        @DisplayName("Should validate tenure against loan type maximum")
        void shouldValidateTenureAgainstLoanType() {
            assertThatThrownBy(() -> LoanApplication.builder()
                    .customerId(customerId)
                    .loanType(LoanType.PERSONAL_LOAN) // Max 7 years = 84 months
                    .requestedAmount(new BigDecimal("500000"))
                    .tenureMonths(120) // 10 years - exceeds max
                    .purpose("Test")
                    .build()
                    .validate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum tenure");
        }
    }
}
