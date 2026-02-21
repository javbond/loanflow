package com.loanflow.policy.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluation context containing all data needed to evaluate policies against a loan application.
 * Uses a flat map structure with dot-notation keys for flexible field access.
 *
 * Standard field paths:
 *   loan.type, loan.requestedAmount, loan.tenureMonths, loan.purpose, loan.branchCode
 *   applicant.cibilScore, applicant.riskCategory, applicant.age
 *   applicant.employmentType, applicant.monthlyIncome, applicant.yearsOfExperience
 *   property.estimatedValue, property.type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    /**
     * Flat map of field path → value.
     * All values are stored as strings and parsed during evaluation.
     */
    @Builder.Default
    private Map<String, String> data = new HashMap<>();

    /**
     * Add a field value to the context
     */
    public EvaluationContext put(String field, Object value) {
        if (value != null) {
            data.put(field, String.valueOf(value));
        }
        return this;
    }

    /**
     * Get a field value as string
     */
    public String get(String field) {
        return data.get(field);
    }

    /**
     * Check if a field exists in the context
     */
    public boolean hasField(String field) {
        return data.containsKey(field);
    }

    /**
     * Get a field value as a number (Double)
     */
    public Double getAsNumber(String field) {
        String value = data.get(field);
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get a field value as boolean
     */
    public Boolean getAsBoolean(String field) {
        String value = data.get(field);
        if (value == null || value.isBlank()) return null;
        return Boolean.parseBoolean(value);
    }
}
