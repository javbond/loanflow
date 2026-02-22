package com.loanflow.loan.incomeverification.client;

import com.loanflow.loan.incomeverification.dto.IncomeVerificationRequest;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationResponse;

/**
 * Interface for income verification API client.
 * Implementations:
 * - MockIncomeVerificationApiClient (@Profile dev/uat/test) — deterministic mock
 * - RealIncomeVerificationApiClient (@Profile prod/staging) — actual API (future)
 */
public interface IncomeVerificationApiClient {

    /**
     * Verify income using PAN (ITR), GSTIN (GST), and bank statement analysis.
     *
     * @param request Income verification request with PAN and optional GSTIN
     * @return Income verification response with ITR, GST, and bank data
     */
    IncomeVerificationResponse verifyIncome(IncomeVerificationRequest request);
}
