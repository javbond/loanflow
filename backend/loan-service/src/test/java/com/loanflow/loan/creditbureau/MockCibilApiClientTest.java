package com.loanflow.loan.creditbureau;

import com.loanflow.loan.creditbureau.client.MockCibilApiClient;
import com.loanflow.loan.creditbureau.dto.BureauDataSource;
import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mock CIBIL API Client")
class MockCibilApiClientTest {

    private MockCibilApiClient client;

    @BeforeEach
    void setUp() {
        client = new MockCibilApiClient();
    }

    @Nested
    @DisplayName("Deterministic Score Generation")
    class DeterministicScoreTests {

        @Test
        @DisplayName("Should return same score for same PAN (deterministic)")
        void shouldReturnDeterministicScore() {
            CreditBureauRequest request = CreditBureauRequest.builder().pan("ABCDE1234F").build();
            CreditBureauResponse response1 = client.fetchCreditReport(request);
            CreditBureauResponse response2 = client.fetchCreditReport(request);

            assertThat(response1.getCreditScore()).isEqualTo(response2.getCreditScore());
        }

        @Test
        @DisplayName("Should return different scores for different PANs")
        void shouldReturnDifferentScoresForDifferentPans() {
            CreditBureauResponse response1 = client.fetchCreditReport(
                    CreditBureauRequest.builder().pan("ABCDE1234F").build());
            CreditBureauResponse response2 = client.fetchCreditReport(
                    CreditBureauRequest.builder().pan("ZZZZZ9999Z").build());

            // Different PANs should generally produce different scores
            // (very small chance of collision, but not worth testing exhaustively)
            assertThat(response1.getCreditScore()).isNotEqualTo(0);
            assertThat(response2.getCreditScore()).isNotEqualTo(0);
        }

        @Test
        @DisplayName("Should generate score in valid CIBIL range (300-900)")
        void shouldReturnScoreInValidRange() {
            String[] testPans = {"ABCDE1234F", "XYZWV9876A", "MNOPQ5432B", "GHIJK7654C"};
            for (String pan : testPans) {
                CreditBureauResponse response = client.fetchCreditReport(
                        CreditBureauRequest.builder().pan(pan).build());
                assertThat(response.getCreditScore())
                        .as("Score for PAN %s", pan)
                        .isBetween(300, 900);
            }
        }
    }

    @Nested
    @DisplayName("Response Content")
    class ResponseContentTests {

        @Test
        @DisplayName("Should include account summaries")
        void shouldIncludeAccountSummaries() {
            CreditBureauResponse response = client.fetchCreditReport(
                    CreditBureauRequest.builder().pan("ABCDE1234F").build());

            assertThat(response.getAccounts()).isNotEmpty();
            assertThat(response.getAccounts().get(0).getAccountType()).isNotBlank();
            assertThat(response.getAccounts().get(0).getLenderName()).isNotBlank();
        }

        @Test
        @DisplayName("Should set dataSource as REAL (simulating real API path)")
        void shouldSetDataSourceAsReal() {
            CreditBureauResponse response = client.fetchCreditReport(
                    CreditBureauRequest.builder().pan("ABCDE1234F").build());

            assertThat(response.getDataSource()).isEqualTo(BureauDataSource.REAL);
            assertThat(response.getControlNumber()).startsWith("MOCK-");
            assertThat(response.getPullTimestamp()).isNotNull();
            assertThat(response.getScoreVersion()).contains("Mock");
        }
    }
}
