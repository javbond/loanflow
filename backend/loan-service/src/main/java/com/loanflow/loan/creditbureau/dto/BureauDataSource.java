package com.loanflow.loan.creditbureau.dto;

/**
 * Tracks the origin of credit bureau data.
 * REAL = fresh pull from CIBIL API,
 * CACHED = from Redis cache (within TTL),
 * SIMULATED = fallback when API unavailable.
 */
public enum BureauDataSource {
    REAL,
    CACHED,
    SIMULATED
}
