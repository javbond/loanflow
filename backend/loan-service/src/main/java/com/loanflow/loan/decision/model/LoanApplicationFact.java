package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Drools fact representing a loan application.
 * Used by eligibility and pricing DRL rules.
 * Separate from JPA entity — lightweight POJO for rule evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationFact {

    private String id;
    private String applicationNumber;

    /** Product code: PL, HL, VL, GL, KCC, EL, BL, LAP */
    private String productCode;

    private double requestedAmount;
    private int tenureMonths;

    // Home Loan specific
    private double propertyValue;

    // Vehicle Loan specific
    private String vehicleType;  // NEW, USED
    private int vehicleAge;
    private double vehiclePrice;

    // Gold Loan specific
    private double goldValue;
    private double goldWeightGrams;

    /**
     * Calculated EMI based on requested amount, interest rate, and tenure.
     * Used by FOIR calculation rules.
     */
    private double calculatedEmi;

    /**
     * Calculate EMI using standard formula: EMI = P * R * (1+R)^N / ((1+R)^N - 1)
     * where P = principal, R = monthly rate, N = tenure months
     */
    public double getCalculatedEmi() {
        if (calculatedEmi > 0) {
            return calculatedEmi;
        }
        // Default EMI estimation at 12% per annum for FOIR check
        double annualRate = 12.0;
        double monthlyRate = annualRate / 12 / 100;
        double emi = (requestedAmount * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths))
                / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
        return Double.isFinite(emi) ? emi : 0;
    }
}
