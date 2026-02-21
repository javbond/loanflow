package com.loanflow.loan.dashboard;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating risk analytics from loan application data.
 *
 * Provides metrics for the Risk Dashboard:
 * - CIBIL score distribution (buckets)
 * - Risk tier breakdown (A/B/C/D → LOW/MEDIUM/MEDIUM_HIGH/HIGH)
 * - Negative marker alerts (NPA, high-risk rejections)
 * - Summary KPIs (average score, high-risk count, portfolio exposure)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RiskDashboardService {

    private final LoanApplicationRepository loanRepository;

    /**
     * Compile the full risk dashboard response.
     */
    public RiskDashboardResponse getDashboard() {
        log.info("Compiling risk dashboard data");

        List<RiskDashboardResponse.ScoreDistribution> scoreDistribution = getScoreDistribution();
        List<RiskDashboardResponse.RiskTierBreakdown> riskTierBreakdown = getRiskTierBreakdown();
        List<RiskDashboardResponse.NegativeMarkerAlert> negativeMarkers = getNegativeMarkerAlerts();
        RiskDashboardResponse.SummaryKpis summaryKpis = getSummaryKpis();
        List<RiskDashboardResponse.LoanTypeRiskBreakdown> loanTypeRisk = getLoanTypeRiskBreakdown();
        List<RiskDashboardResponse.PortfolioExposure> portfolioExposure = getPortfolioExposure();

        return RiskDashboardResponse.builder()
                .scoreDistribution(scoreDistribution)
                .riskTierBreakdown(riskTierBreakdown)
                .negativeMarkerAlerts(negativeMarkers)
                .summaryKpis(summaryKpis)
                .loanTypeRiskBreakdown(loanTypeRisk)
                .portfolioExposure(portfolioExposure)
                .generatedAt(java.time.Instant.now())
                .build();
    }

    /**
     * Get CIBIL score distribution in defined buckets.
     */
    public List<RiskDashboardResponse.ScoreDistribution> getScoreDistribution() {
        List<Object[]> rawData = loanRepository.countByCibilScoreRange();

        // Define all buckets with their display order
        Map<String, Long> bucketMap = new LinkedHashMap<>();
        bucketMap.put("EXCELLENT", 0L);
        bucketMap.put("GOOD", 0L);
        bucketMap.put("FAIR", 0L);
        bucketMap.put("BELOW_AVERAGE", 0L);
        bucketMap.put("POOR", 0L);

        for (Object[] row : rawData) {
            String range = (String) row[0];
            Long count = (Long) row[1];
            bucketMap.put(range, count);
        }

        Map<String, String> rangeLabels = Map.of(
                "EXCELLENT", "750+",
                "GOOD", "700-749",
                "FAIR", "650-699",
                "BELOW_AVERAGE", "550-649",
                "POOR", "Below 550"
        );

        Map<String, String> rangeColors = Map.of(
                "EXCELLENT", "#22c55e",
                "GOOD", "#84cc16",
                "FAIR", "#eab308",
                "BELOW_AVERAGE", "#f97316",
                "POOR", "#ef4444"
        );

        return bucketMap.entrySet().stream()
                .map(entry -> new RiskDashboardResponse.ScoreDistribution(
                        entry.getKey(),
                        rangeLabels.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue(),
                        rangeColors.getOrDefault(entry.getKey(), "#6b7280")))
                .toList();
    }

    /**
     * Get risk tier breakdown (count + percentage per risk category).
     */
    public List<RiskDashboardResponse.RiskTierBreakdown> getRiskTierBreakdown() {
        List<Object[]> rawData = loanRepository.countByRiskCategory();

        long total = rawData.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();

        Map<String, String> tierColors = Map.of(
                "LOW", "#22c55e",
                "MEDIUM", "#3b82f6",
                "MEDIUM_HIGH", "#f97316",
                "HIGH", "#ef4444"
        );

        Map<String, String> tierLabels = Map.of(
                "LOW", "Low Risk",
                "MEDIUM", "Medium Risk",
                "MEDIUM_HIGH", "Medium-High Risk",
                "HIGH", "High Risk"
        );

        return rawData.stream()
                .map(row -> {
                    String category = (String) row[0];
                    long count = (Long) row[1];
                    double percentage = total > 0 ? (count * 100.0) / total : 0;
                    return new RiskDashboardResponse.RiskTierBreakdown(
                            category,
                            tierLabels.getOrDefault(category, category),
                            count,
                            Math.round(percentage * 100.0) / 100.0,
                            tierColors.getOrDefault(category, "#6b7280"));
                })
                .sorted(Comparator.comparingInt(t -> tierOrder(t.category())))
                .toList();
    }

    /**
     * Get negative marker alerts — NPA applications and high-risk rejections.
     */
    public List<RiskDashboardResponse.NegativeMarkerAlert> getNegativeMarkerAlerts() {
        List<LoanApplication> negativeApps = loanRepository.findNegativeMarkerApplications();

        return negativeApps.stream()
                .limit(20) // Cap at 20 most recent
                .map(app -> new RiskDashboardResponse.NegativeMarkerAlert(
                        app.getId().toString(),
                        app.getApplicationNumber(),
                        app.getLoanType().getDisplayName(),
                        app.getRequestedAmount(),
                        app.getCibilScore(),
                        app.getRiskCategory(),
                        app.getStatus().name(),
                        determineAlertType(app),
                        determineAlertSeverity(app),
                        app.getRejectionReason(),
                        app.getUpdatedAt()))
                .toList();
    }

    /**
     * Get summary KPI metrics.
     */
    public RiskDashboardResponse.SummaryKpis getSummaryKpis() {
        Double avgScore = loanRepository.findAverageCibilScore();
        long highRiskCount = loanRepository.countHighRiskApplications();

        List<Object[]> statusCounts = loanRepository.countByStatus();
        long totalProcessed = statusCounts.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();

        // Count NPA
        long npaCount = statusCounts.stream()
                .filter(row -> "NPA".equals(row[0].toString()))
                .mapToLong(row -> (Long) row[1])
                .sum();

        // Count rejections
        long rejectedCount = statusCounts.stream()
                .filter(row -> "REJECTED".equals(row[0].toString()))
                .mapToLong(row -> (Long) row[1])
                .sum();

        double rejectionRate = totalProcessed > 0
                ? Math.round((rejectedCount * 100.0 / totalProcessed) * 100.0) / 100.0
                : 0;

        return new RiskDashboardResponse.SummaryKpis(
                avgScore != null ? Math.round(avgScore) : 0,
                highRiskCount,
                totalProcessed,
                npaCount,
                rejectedCount,
                rejectionRate);
    }

    /**
     * Get risk breakdown per loan type (cross-analysis).
     */
    public List<RiskDashboardResponse.LoanTypeRiskBreakdown> getLoanTypeRiskBreakdown() {
        List<Object[]> rawData = loanRepository.countByLoanTypeAndRiskCategory();

        // Group by loan type
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String loanType = row[0].toString();
            String riskCategory = (String) row[1];
            Long count = (Long) row[2];

            grouped.computeIfAbsent(loanType, k -> new LinkedHashMap<>())
                    .put(riskCategory, count);
        }

        return grouped.entrySet().stream()
                .map(entry -> new RiskDashboardResponse.LoanTypeRiskBreakdown(
                        entry.getKey(),
                        entry.getValue()))
                .toList();
    }

    /**
     * Get portfolio exposure by risk category.
     */
    public List<RiskDashboardResponse.PortfolioExposure> getPortfolioExposure() {
        List<Object[]> rawData = loanRepository.sumApprovedAmountByRiskCategory();

        BigDecimal totalExposure = rawData.stream()
                .map(row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rawData.stream()
                .map(row -> {
                    String category = (String) row[0];
                    BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    double percentage = totalExposure.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(new BigDecimal("100"))
                            .divide(totalExposure, 2, RoundingMode.HALF_UP).doubleValue()
                            : 0;
                    return new RiskDashboardResponse.PortfolioExposure(
                            category, amount, percentage);
                })
                .sorted(Comparator.comparingInt(e -> tierOrder(e.riskCategory())))
                .toList();
    }

    // ======================== HELPERS ========================

    private String determineAlertType(LoanApplication app) {
        if (app.getStatus().name().equals("NPA")) return "NPA";
        if (app.getCibilScore() != null && app.getCibilScore() < 550) return "LOW_CREDIT_SCORE";
        if ("HIGH".equals(app.getRiskCategory())) return "HIGH_RISK_REJECTION";
        return "RISK_FLAG";
    }

    private String determineAlertSeverity(LoanApplication app) {
        if (app.getStatus().name().equals("NPA")) return "CRITICAL";
        if (app.getCibilScore() != null && app.getCibilScore() < 550) return "HIGH";
        if ("HIGH".equals(app.getRiskCategory())) return "HIGH";
        return "MEDIUM";
    }

    private int tierOrder(String category) {
        return switch (category) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "MEDIUM_HIGH" -> 3;
            case "HIGH" -> 4;
            default -> 5;
        };
    }
}
