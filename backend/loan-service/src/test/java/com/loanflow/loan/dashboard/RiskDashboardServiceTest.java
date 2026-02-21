package com.loanflow.loan.dashboard;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.LoanApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * TDD unit tests for RiskDashboardService.
 * Tests aggregation logic for risk analytics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskDashboardService Tests")
class RiskDashboardServiceTest {

    @Mock
    private LoanApplicationRepository loanRepository;

    @InjectMocks
    private RiskDashboardService service;

    @Nested
    @DisplayName("Score Distribution")
    class ScoreDistributionTests {

        @Test
        @DisplayName("Should return all five score buckets")
        void shouldReturnAllFiveBuckets() {
            List<Object[]> data = new java.util.ArrayList<>();
            data.add(new Object[]{"EXCELLENT", 10L});
            data.add(new Object[]{"GOOD", 15L});
            data.add(new Object[]{"FAIR", 8L});
            data.add(new Object[]{"BELOW_AVERAGE", 5L});
            data.add(new Object[]{"POOR", 2L});
            when(loanRepository.countByCibilScoreRange()).thenReturn(data);

            List<RiskDashboardResponse.ScoreDistribution> result = service.getScoreDistribution();

            assertThat(result).hasSize(5);
            assertThat(result.get(0).range()).isEqualTo("EXCELLENT");
            assertThat(result.get(0).count()).isEqualTo(10L);
            assertThat(result.get(0).label()).isEqualTo("750+");
        }

        @Test
        @DisplayName("Should return zero counts for missing buckets")
        void shouldReturnZeroForMissingBuckets() {
            List<Object[]> data = new java.util.ArrayList<>();
            data.add(new Object[]{"EXCELLENT", 5L});
            when(loanRepository.countByCibilScoreRange()).thenReturn(data);

            List<RiskDashboardResponse.ScoreDistribution> result = service.getScoreDistribution();

            assertThat(result).hasSize(5);
            // GOOD, FAIR, BELOW_AVERAGE, POOR should be 0
            long zeroCount = result.stream()
                    .filter(d -> d.count() == 0)
                    .count();
            assertThat(zeroCount).isEqualTo(4);
        }

        @Test
        @DisplayName("Should handle empty result set")
        void shouldHandleEmptyResultSet() {
            List<Object[]> empty = new java.util.ArrayList<>();
            when(loanRepository.countByCibilScoreRange()).thenReturn(empty);

            List<RiskDashboardResponse.ScoreDistribution> result = service.getScoreDistribution();

            assertThat(result).hasSize(5);
            assertThat(result).allMatch(d -> d.count() == 0);
        }
    }

    @Nested
    @DisplayName("Risk Tier Breakdown")
    class RiskTierBreakdownTests {

        @Test
        @DisplayName("Should calculate percentages correctly")
        void shouldCalculatePercentages() {
            List<Object[]> data = new java.util.ArrayList<>();
            data.add(new Object[]{"LOW", 40L});
            data.add(new Object[]{"MEDIUM", 30L});
            data.add(new Object[]{"MEDIUM_HIGH", 20L});
            data.add(new Object[]{"HIGH", 10L});
            when(loanRepository.countByRiskCategory()).thenReturn(data);

            List<RiskDashboardResponse.RiskTierBreakdown> result = service.getRiskTierBreakdown();

            assertThat(result).hasSize(4);
            // LOW should be 40%
            RiskDashboardResponse.RiskTierBreakdown low = result.stream()
                    .filter(t -> "LOW".equals(t.category()))
                    .findFirst()
                    .orElseThrow();
            assertThat(low.percentage()).isEqualTo(40.0);
            assertThat(low.label()).isEqualTo("Low Risk");
        }

        @Test
        @DisplayName("Should sort by tier order (LOW → HIGH)")
        void shouldSortByTierOrder() {
            List<Object[]> data = new java.util.ArrayList<>();
            data.add(new Object[]{"HIGH", 5L});
            data.add(new Object[]{"LOW", 20L});
            data.add(new Object[]{"MEDIUM", 15L});
            when(loanRepository.countByRiskCategory()).thenReturn(data);

            List<RiskDashboardResponse.RiskTierBreakdown> result = service.getRiskTierBreakdown();

            assertThat(result.get(0).category()).isEqualTo("LOW");
            assertThat(result.get(1).category()).isEqualTo("MEDIUM");
            assertThat(result.get(2).category()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should handle empty risk categories")
        void shouldHandleEmptyCategories() {
            List<Object[]> empty = new java.util.ArrayList<>();
            when(loanRepository.countByRiskCategory()).thenReturn(empty);

            List<RiskDashboardResponse.RiskTierBreakdown> result = service.getRiskTierBreakdown();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Negative Marker Alerts")
    class NegativeMarkerAlertTests {

        @Test
        @DisplayName("Should identify NPA applications")
        void shouldIdentifyNpaApplications() {
            LoanApplication npaApp = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2025-000001")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("500000"))
                    .cibilScore(450)
                    .riskCategory("HIGH")
                    .status(LoanStatus.NPA)
                    .updatedAt(Instant.now())
                    .build();

            when(loanRepository.findNegativeMarkerApplications())
                    .thenReturn(List.of(npaApp));

            List<RiskDashboardResponse.NegativeMarkerAlert> result = service.getNegativeMarkerAlerts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo("NPA");
            assertThat(result.get(0).severity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("Should identify high-risk rejections")
        void shouldIdentifyHighRiskRejections() {
            LoanApplication rejectedApp = LoanApplication.builder()
                    .id(UUID.randomUUID())
                    .applicationNumber("LN-2025-000002")
                    .loanType(LoanType.HOME_LOAN)
                    .requestedAmount(new BigDecimal("5000000"))
                    .cibilScore(620)
                    .riskCategory("HIGH")
                    .status(LoanStatus.REJECTED)
                    .rejectionReason("Poor credit history")
                    .updatedAt(Instant.now())
                    .build();

            when(loanRepository.findNegativeMarkerApplications())
                    .thenReturn(List.of(rejectedApp));

            List<RiskDashboardResponse.NegativeMarkerAlert> result = service.getNegativeMarkerAlerts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).alertType()).isEqualTo("HIGH_RISK_REJECTION");
            assertThat(result.get(0).severity()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should cap alerts at 20")
        void shouldCapAlertsAt20() {
            List<LoanApplication> manyApps = new java.util.ArrayList<>();
            for (int i = 0; i < 30; i++) {
                manyApps.add(LoanApplication.builder()
                        .id(UUID.randomUUID())
                        .applicationNumber("LN-2025-" + String.format("%06d", i))
                        .loanType(LoanType.PERSONAL_LOAN)
                        .requestedAmount(new BigDecimal("100000"))
                        .cibilScore(400)
                        .riskCategory("HIGH")
                        .status(LoanStatus.NPA)
                        .updatedAt(Instant.now())
                        .build());
            }

            when(loanRepository.findNegativeMarkerApplications())
                    .thenReturn(manyApps);

            List<RiskDashboardResponse.NegativeMarkerAlert> result = service.getNegativeMarkerAlerts();

            assertThat(result).hasSize(20);
        }
    }

    @Nested
    @DisplayName("Summary KPIs")
    class SummaryKpiTests {

        @Test
        @DisplayName("Should calculate summary metrics correctly")
        void shouldCalculateSummaryMetrics() {
            when(loanRepository.findAverageCibilScore()).thenReturn(712.5);
            when(loanRepository.countHighRiskApplications()).thenReturn(8L);

            List<Object[]> statusCounts = new java.util.ArrayList<>();
            statusCounts.add(new Object[]{LoanStatus.APPROVED, 50L});
            statusCounts.add(new Object[]{LoanStatus.REJECTED, 10L});
            statusCounts.add(new Object[]{LoanStatus.DISBURSED, 30L});
            statusCounts.add(new Object[]{LoanStatus.NPA, 2L});
            statusCounts.add(new Object[]{LoanStatus.UNDERWRITING, 8L});
            when(loanRepository.countByStatus()).thenReturn(statusCounts);

            RiskDashboardResponse.SummaryKpis result = service.getSummaryKpis();

            assertThat(result.averageCibilScore()).isEqualTo(713); // Rounded
            assertThat(result.highRiskCount()).isEqualTo(8L);
            assertThat(result.totalProcessed()).isEqualTo(100L);
            assertThat(result.npaCount()).isEqualTo(2L);
            assertThat(result.rejectedCount()).isEqualTo(10L);
            assertThat(result.rejectionRate()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should handle null average CIBIL score")
        void shouldHandleNullAverageCibilScore() {
            when(loanRepository.findAverageCibilScore()).thenReturn(null);
            when(loanRepository.countHighRiskApplications()).thenReturn(0L);
            List<Object[]> emptyList = new java.util.ArrayList<>();
            when(loanRepository.countByStatus()).thenReturn(emptyList);

            RiskDashboardResponse.SummaryKpis result = service.getSummaryKpis();

            assertThat(result.averageCibilScore()).isEqualTo(0L);
            assertThat(result.rejectionRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Portfolio Exposure")
    class PortfolioExposureTests {

        @Test
        @DisplayName("Should calculate exposure percentages")
        void shouldCalculateExposurePercentages() {
            List<Object[]> data = new java.util.ArrayList<>();
            data.add(new Object[]{"LOW", new BigDecimal("50000000")});
            data.add(new Object[]{"MEDIUM", new BigDecimal("30000000")});
            data.add(new Object[]{"HIGH", new BigDecimal("20000000")});
            when(loanRepository.sumApprovedAmountByRiskCategory()).thenReturn(data);

            List<RiskDashboardResponse.PortfolioExposure> result = service.getPortfolioExposure();

            assertThat(result).hasSize(3);
            // LOW: 50M / 100M = 50%
            RiskDashboardResponse.PortfolioExposure low = result.stream()
                    .filter(e -> "LOW".equals(e.riskCategory()))
                    .findFirst()
                    .orElseThrow();
            assertThat(low.percentage()).isEqualTo(50.0);
            assertThat(low.totalAmount()).isEqualByComparingTo(new BigDecimal("50000000"));
        }
    }

    @Nested
    @DisplayName("Full Dashboard")
    class FullDashboardTests {

        @Test
        @DisplayName("Should compile complete dashboard response")
        void shouldCompileCompleteDashboard() {
            // Setup all mocks
            List<Object[]> scoreRanges = new java.util.ArrayList<>();
            scoreRanges.add(new Object[]{"EXCELLENT", 5L});
            when(loanRepository.countByCibilScoreRange()).thenReturn(scoreRanges);

            List<Object[]> riskCategories = new java.util.ArrayList<>();
            riskCategories.add(new Object[]{"LOW", 10L});
            when(loanRepository.countByRiskCategory()).thenReturn(riskCategories);

            when(loanRepository.findNegativeMarkerApplications()).thenReturn(List.of());
            when(loanRepository.findAverageCibilScore()).thenReturn(720.0);
            when(loanRepository.countHighRiskApplications()).thenReturn(3L);

            List<Object[]> statusCounts = new java.util.ArrayList<>();
            statusCounts.add(new Object[]{LoanStatus.APPROVED, 20L});
            when(loanRepository.countByStatus()).thenReturn(statusCounts);

            List<Object[]> emptyObjList = new java.util.ArrayList<>();
            when(loanRepository.countByLoanTypeAndRiskCategory()).thenReturn(emptyObjList);
            when(loanRepository.sumApprovedAmountByRiskCategory()).thenReturn(new java.util.ArrayList<>());

            RiskDashboardResponse result = service.getDashboard();

            assertThat(result).isNotNull();
            assertThat(result.scoreDistribution()).isNotNull();
            assertThat(result.riskTierBreakdown()).isNotNull();
            assertThat(result.negativeMarkerAlerts()).isNotNull();
            assertThat(result.summaryKpis()).isNotNull();
            assertThat(result.loanTypeRiskBreakdown()).isNotNull();
            assertThat(result.portfolioExposure()).isNotNull();
            assertThat(result.generatedAt()).isNotNull();
        }
    }
}
