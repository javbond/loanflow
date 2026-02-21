package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Drools fact for pricing determination output.
 * Inserted empty into working memory; pricing rules populate it with rates, discounts, premiums.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResultFact {

    private String applicationId;

    /** Base rate = Repo Rate + Product Spread */
    private double baseRate;

    /** Product spread over repo rate */
    private double productSpread;

    /** Risk tier: A (excellent), B (good), C (fair), D (below average) */
    private String riskTier;

    /** Named discounts (negative values reduce rate) */
    @Builder.Default
    private Map<String, Double> discounts = new LinkedHashMap<>();

    /** Named premiums (positive values increase rate) */
    @Builder.Default
    private Map<String, Double> premiums = new LinkedHashMap<>();

    /** Final interest rate after all adjustments */
    private double finalInterestRate;

    /** Processing fee in absolute amount */
    private double processingFee;

    /** Processing fee as percentage */
    private double processingFeePercent;

    /** Processing fee waiver amount (for premium customers) */
    private double processingFeeWaiver;

    /** Calculated EMI at final rate */
    private double emi;

    // Product rate floors and caps
    @Builder.Default
    private double productMinRate = 7.0;
    @Builder.Default
    private double productMaxRate = 24.0;

    // Loan details for EMI calculation
    private double loanAmount;
    private int tenureMonths;

    // ---- Methods called by DRL rules ----

    public void addDiscount(String name, double value) {
        if (discounts == null) {
            discounts = new LinkedHashMap<>();
        }
        discounts.put(name, value);
    }

    public void addPremium(String name, double value) {
        if (premiums == null) {
            premiums = new LinkedHashMap<>();
        }
        premiums.put(name, value);
    }

    public Map<String, Double> getDiscounts() {
        if (discounts == null) {
            discounts = new LinkedHashMap<>();
        }
        return discounts;
    }

    public Map<String, Double> getPremiums() {
        if (premiums == null) {
            premiums = new LinkedHashMap<>();
        }
        return premiums;
    }

    public double getTotalDiscounts() {
        if (discounts == null || discounts.isEmpty()) return 0;
        return discounts.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getTotalPremiums() {
        if (premiums == null || premiums.isEmpty()) return 0;
        return premiums.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Calculate EMI using standard formula: EMI = P * R * (1+R)^N / ((1+R)^N - 1)
     * Called by the final pricing rule after finalInterestRate is determined.
     */
    public void calculateEmi() {
        if (finalInterestRate <= 0 || loanAmount <= 0 || tenureMonths <= 0) {
            return;
        }
        double monthlyRate = finalInterestRate / 12.0 / 100.0;
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        this.emi = (loanAmount * monthlyRate * factor) / (factor - 1);
    }
}
