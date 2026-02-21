package com.loanflow.loan.decision.service;

import org.springframework.stereotype.Service;

/**
 * Configuration service used as a Drools global in eligibility rules.
 * Provides configurable thresholds for rule evaluation.
 *
 * Currently returns hardcoded defaults; can be enhanced to read from
 * database or application configuration in future sprints.
 */
@Service
public class ConfigService {

    public int getMinAge() {
        return 21;
    }

    public int getMaxAgeAtMaturity() {
        return 65;
    }

    /**
     * Maximum Fixed Obligation to Income Ratio (FOIR)
     */
    public double getMaxFoir() {
        return 0.50;
    }

    /**
     * Minimum CIBIL score by product code
     */
    public int getMinCibilScore(String productCode) {
        return switch (productCode) {
            case "HL" -> 650;
            case "PL" -> 650;
            case "VL" -> 650;
            case "GL" -> 550;
            default -> 600;
        };
    }

    /**
     * Minimum income by product and employment type
     */
    public double getMinIncome(String productCode, String employmentType) {
        if ("SALARIED".equals(employmentType)) {
            return switch (productCode) {
                case "HL" -> 40000;
                case "PL" -> 25000;
                case "VL" -> 25000;
                default -> 20000;
            };
        } else {
            return switch (productCode) {
                case "HL" -> 60000;
                case "PL" -> 40000;
                default -> 30000;
            };
        }
    }
}
