package com.loanflow.loan.dashboard;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Complete risk dashboard response containing all analytics sections.
 */
@Builder
public record RiskDashboardResponse(
        List<ScoreDistribution> scoreDistribution,
        List<RiskTierBreakdown> riskTierBreakdown,
        List<NegativeMarkerAlert> negativeMarkerAlerts,
        SummaryKpis summaryKpis,
        List<LoanTypeRiskBreakdown> loanTypeRiskBreakdown,
        List<PortfolioExposure> portfolioExposure,
        Instant generatedAt
) {

    /**
     * CIBIL score distribution bucket.
     */
    public record ScoreDistribution(
            String range,
            String label,
            long count,
            String color
    ) {}

    /**
     * Risk tier breakdown with count and percentage.
     */
    public record RiskTierBreakdown(
            String category,
            String label,
            long count,
            double percentage,
            String color
    ) {}

    /**
     * Individual negative marker alert for an application.
     */
    public record NegativeMarkerAlert(
            String applicationId,
            String applicationNumber,
            String loanType,
            BigDecimal requestedAmount,
            Integer cibilScore,
            String riskCategory,
            String status,
            String alertType,
            String severity,
            String reason,
            Instant lastUpdated
    ) {}

    /**
     * Summary KPI metrics.
     */
    public record SummaryKpis(
            long averageCibilScore,
            long highRiskCount,
            long totalProcessed,
            long npaCount,
            long rejectedCount,
            double rejectionRate
    ) {}

    /**
     * Risk breakdown per loan product type.
     */
    public record LoanTypeRiskBreakdown(
            String loanType,
            Map<String, Long> riskCounts
    ) {}

    /**
     * Portfolio exposure by risk category.
     */
    public record PortfolioExposure(
            String riskCategory,
            BigDecimal totalAmount,
            double percentage
    ) {}
}
