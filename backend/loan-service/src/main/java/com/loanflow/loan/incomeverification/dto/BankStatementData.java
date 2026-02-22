package com.loanflow.loan.incomeverification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Bank statement analysis data — 6-month summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatementData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Average monthly closing balance over 6 months */
    private BigDecimal avgMonthlyBalance;

    /** Average monthly credit (salary/business deposits) */
    private BigDecimal avgMonthlyCredits;

    /** Number of cheque/ECS bounces in 6 months */
    private int bounceCount;

    /** Month-wise balances (most recent first), typically 6 entries */
    private List<BigDecimal> monthlyBalances;

    /** Number of months analyzed */
    private int monthsAnalyzed;

    /** Minimum balance across all months */
    private BigDecimal minBalance;

    /** Maximum balance across all months */
    private BigDecimal maxBalance;
}
