package com.loanflow.loan.incomeverification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Income verification response — aggregates ITR, GST, and bank statement data.
 * Serializable for Redis caching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeVerificationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** PAN used for verification */
    private String pan;

    /** Whether income was successfully verified */
    private boolean incomeVerified;

    /** Verified monthly income (from ITR / bank statements) */
    private BigDecimal verifiedMonthlyIncome;

    /** Debt-to-Income ratio (0.0 to 1.0) */
    private BigDecimal dtiRatio;

    /** Income consistency score (0-100): declared vs verified income match */
    private int incomeConsistencyScore;

    /** ITR verification data */
    private ItrData itrData;

    /** GST verification data (null if not self-employed/business) */
    private GstData gstData;

    /** Bank statement analysis data */
    private BankStatementData bankStatementData;

    /** Warning flags for manual review */
    private List<String> flags;

    /** Data source: REAL, CACHED, SIMULATED */
    private IncomeDataSource dataSource;

    /** Timestamp of verification */
    private Instant verificationTimestamp;
}
