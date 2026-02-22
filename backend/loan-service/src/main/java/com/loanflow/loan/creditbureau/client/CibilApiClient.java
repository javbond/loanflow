package com.loanflow.loan.creditbureau.client;

import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;

/**
 * Interface for CIBIL credit bureau API client.
 * Implementations:
 * - MockCibilApiClient (@Profile dev/uat/test) — deterministic mock
 * - RealCibilApiClient (@Profile prod/staging) — actual CIBIL API (future)
 */
public interface CibilApiClient {

    /**
     * Fetch credit report from CIBIL bureau.
     *
     * @param request Credit bureau request with PAN and applicant details
     * @return Credit bureau response with score, accounts, enquiries
     * @throws CibilApiException if the bureau API call fails
     */
    CreditBureauResponse fetchCreditReport(CreditBureauRequest request);
}
