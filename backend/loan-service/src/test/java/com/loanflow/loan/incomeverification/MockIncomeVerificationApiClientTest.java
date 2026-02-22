package com.loanflow.loan.incomeverification;

import com.loanflow.loan.incomeverification.client.MockIncomeVerificationApiClient;
import com.loanflow.loan.incomeverification.dto.IncomeDataSource;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationRequest;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mock Income Verification API Client")
class MockIncomeVerificationApiClientTest {

    private MockIncomeVerificationApiClient client;

    @BeforeEach
    void setUp() {
        client = new MockIncomeVerificationApiClient();
    }

    @Nested
    @DisplayName("Deterministic Income Generation")
    class DeterministicTests {

        @Test
        @DisplayName("Should return same verified income for same PAN (deterministic)")
        void shouldReturnDeterministicIncome() {
            IncomeVerificationRequest request = IncomeVerificationRequest.builder()
                    .pan("ABCDE1234F")
                    .employmentType("SALARIED")
                    .declaredMonthlyIncome(BigDecimal.valueOf(75000))
                    .build();

            IncomeVerificationResponse r1 = client.verifyIncome(request);
            IncomeVerificationResponse r2 = client.verifyIncome(request);

            assertThat(r1.getVerifiedMonthlyIncome()).isEqualTo(r2.getVerifiedMonthlyIncome());
            assertThat(r1.getDtiRatio()).isEqualTo(r2.getDtiRatio());
            assertThat(r1.getIncomeConsistencyScore()).isEqualTo(r2.getIncomeConsistencyScore());
        }

        @Test
        @DisplayName("Should always include ITR data")
        void shouldAlwaysIncludeItrData() {
            IncomeVerificationRequest request = IncomeVerificationRequest.builder()
                    .pan("XYZWV9876A")
                    .employmentType("SALARIED")
                    .declaredMonthlyIncome(BigDecimal.valueOf(50000))
                    .build();

            IncomeVerificationResponse response = client.verifyIncome(request);

            assertThat(response.getItrData()).isNotNull();
            assertThat(response.getItrData().getGrossTotalIncome()).isPositive();
            assertThat(response.getItrData().getItrFormType()).isNotBlank();
            assertThat(response.getItrData().getAssessmentYear()).isEqualTo("2025-26");
        }
    }

    @Nested
    @DisplayName("GST Data")
    class GstDataTests {

        @Test
        @DisplayName("Should generate GST data only when GSTIN is provided")
        void shouldGenerateGstOnlyWithGstin() {
            // Without GSTIN
            IncomeVerificationRequest noGstin = IncomeVerificationRequest.builder()
                    .pan("ABCDE1234F")
                    .employmentType("SALARIED")
                    .declaredMonthlyIncome(BigDecimal.valueOf(50000))
                    .build();
            assertThat(client.verifyIncome(noGstin).getGstData()).isNull();

            // With GSTIN
            IncomeVerificationRequest withGstin = IncomeVerificationRequest.builder()
                    .pan("ABCDE1234F")
                    .employmentType("BUSINESS")
                    .gstin("27ABCDE1234F1Z5")
                    .declaredMonthlyIncome(BigDecimal.valueOf(100000))
                    .build();
            IncomeVerificationResponse response = client.verifyIncome(withGstin);
            assertThat(response.getGstData()).isNotNull();
            assertThat(response.getGstData().getAnnualTurnover()).isPositive();
            assertThat(response.getGstData().getFilingCount()).isBetween(8, 12);
        }
    }

    @Nested
    @DisplayName("Bank Statement & Consistency")
    class BankStatementTests {

        @Test
        @DisplayName("Should generate 6-month bank statement data")
        void shouldGenerate6MonthBankData() {
            IncomeVerificationRequest request = IncomeVerificationRequest.builder()
                    .pan("MNOPQ5432B")
                    .employmentType("SALARIED")
                    .declaredMonthlyIncome(BigDecimal.valueOf(60000))
                    .build();

            IncomeVerificationResponse response = client.verifyIncome(request);

            assertThat(response.getBankStatementData()).isNotNull();
            assertThat(response.getBankStatementData().getMonthlyBalances()).hasSize(6);
            assertThat(response.getBankStatementData().getMonthsAnalyzed()).isEqualTo(6);
            assertThat(response.getBankStatementData().getAvgMonthlyBalance()).isPositive();
        }

        @Test
        @DisplayName("Should calculate income consistency score (0-100)")
        void shouldCalculateConsistencyScore() {
            IncomeVerificationRequest request = IncomeVerificationRequest.builder()
                    .pan("GHIJK7654C")
                    .employmentType("SALARIED")
                    .declaredMonthlyIncome(BigDecimal.valueOf(50000))
                    .build();

            IncomeVerificationResponse response = client.verifyIncome(request);

            assertThat(response.getIncomeConsistencyScore()).isBetween(0, 100);
            assertThat(response.isIncomeVerified()).isTrue();
            assertThat(response.getDataSource()).isEqualTo(IncomeDataSource.REAL);
            assertThat(response.getVerificationTimestamp()).isNotNull();
        }
    }
}
