package com.loanflow.loan.incomeverification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * GST verification data — only populated for self-employed/business applicants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** GSTIN number */
    private String gstin;

    /** Annual turnover from GST returns */
    private BigDecimal annualTurnover;

    /** GST compliance rating: EXCELLENT, GOOD, FAIR, POOR */
    private String complianceRating;

    /** Number of GST filings in last 12 months (max 12) */
    private int filingCount;

    /** Whether GSTIN is active */
    private boolean active;
}
