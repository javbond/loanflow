package com.loanflow.loan.decision.service;

import org.springframework.stereotype.Service;

/**
 * RBI rate service used as a Drools global in pricing rules.
 * Provides the current RBI repo rate for base rate calculations.
 *
 * Currently returns a hardcoded value; can be enhanced to fetch
 * from an external API or database in future sprints.
 */
@Service
public class RbiRateService {

    /**
     * Current RBI repo rate (as of Feb 2026).
     * Base rate = Repo Rate + Product Spread
     */
    public double getRepoRate() {
        return 6.50;
    }

    /**
     * Get the reverse repo rate
     */
    public double getReverseRepoRate() {
        return 3.35;
    }

    /**
     * Get the marginal standing facility rate
     */
    public double getMsfRate() {
        return 6.75;
    }
}
