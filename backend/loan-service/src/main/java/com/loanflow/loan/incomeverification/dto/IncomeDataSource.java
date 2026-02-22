package com.loanflow.loan.incomeverification.dto;

/**
 * Tracks the origin of income verification data.
 * REAL = fresh pull from verification API,
 * CACHED = from Redis cache (within TTL),
 * SIMULATED = fallback when API unavailable.
 */
public enum IncomeDataSource {
    REAL,
    CACHED,
    SIMULATED
}
