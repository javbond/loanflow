package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drools fact representing collateral for secured loans.
 * Used by pricing rules for LTV-based pricing adjustments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollateralFact {

    private String id;
    private String applicationId;

    private double marketValue;
    private String collateralType;  // PROPERTY, VEHICLE, GOLD, etc.
}
