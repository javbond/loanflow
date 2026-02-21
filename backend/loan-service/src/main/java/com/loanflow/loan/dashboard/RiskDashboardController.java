package com.loanflow.loan.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the Risk Analytics Dashboard.
 *
 * Provides endpoints for risk metrics aggregation used by the
 * frontend Risk Dashboard component. Access is restricted to
 * staff roles (Underwriter+).
 */
@RestController
@RequestMapping("/api/v1/risk/dashboard")
@RequiredArgsConstructor
@Slf4j
public class RiskDashboardController {

    private final RiskDashboardService dashboardService;

    /**
     * Get the complete risk dashboard data.
     * Includes: score distribution, risk tier breakdown, negative markers,
     * summary KPIs, loan type risk analysis, and portfolio exposure.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<RiskDashboardResponse> getDashboard() {
        log.info("Risk dashboard data requested");
        RiskDashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get only the CIBIL score distribution.
     */
    @GetMapping("/score-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<RiskDashboardResponse.ScoreDistribution>> getScoreDistribution() {
        return ResponseEntity.ok(dashboardService.getScoreDistribution());
    }

    /**
     * Get only the risk tier breakdown.
     */
    @GetMapping("/risk-tiers")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<RiskDashboardResponse.RiskTierBreakdown>> getRiskTiers() {
        return ResponseEntity.ok(dashboardService.getRiskTierBreakdown());
    }

    /**
     * Get only the negative marker alerts.
     */
    @GetMapping("/negative-markers")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<List<RiskDashboardResponse.NegativeMarkerAlert>> getNegativeMarkers() {
        return ResponseEntity.ok(dashboardService.getNegativeMarkerAlerts());
    }

    /**
     * Get only the summary KPIs.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<RiskDashboardResponse.SummaryKpis> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummaryKpis());
    }
}
