package com.loanflow.loan.creditbureau.service;

import com.loanflow.loan.creditbureau.client.CibilApiClient;
import com.loanflow.loan.creditbureau.config.CibilProperties;
import com.loanflow.loan.creditbureau.dto.BureauDataSource;
import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Core credit bureau service — orchestrates CIBIL pulls with caching and fallback.
 *
 * Flow:
 * 1. Check Redis cache by PAN
 * 2. If hit → return with dataSource=CACHED
 * 3. If miss → call CIBIL API with retry
 * 4. On success → cache in Redis with TTL, return with dataSource=REAL
 * 5. On failure → return simulated fallback with dataSource=SIMULATED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditBureauService {

    private final CibilApiClient cibilApiClient;
    private final RedisTemplate<String, Object> creditBureauRedisTemplate;
    private final CibilProperties properties;

    private static final String CACHE_KEY_PREFIX = "CIBIL:";

    /**
     * Pull credit report with cache-first strategy.
     */
    public CreditBureauResponse pullReport(CreditBureauRequest request) {
        String pan = request.getPan();
        String cacheKey = CACHE_KEY_PREFIX + pan;

        // 1. Check Redis cache
        CreditBureauResponse cached = getCachedResponse(cacheKey);
        if (cached != null) {
            log.info("Credit Bureau: Cache HIT for PAN {}***", pan.substring(0, 3));
            cached.setDataSource(BureauDataSource.CACHED);
            return cached;
        }

        // 2. Cache miss — call CIBIL API with retry
        log.info("Credit Bureau: Cache MISS for PAN {}***, calling CIBIL API", pan.substring(0, 3));
        try {
            CreditBureauResponse response = pullWithRetry(request);
            response.setDataSource(BureauDataSource.REAL);
            response.setPullTimestamp(Instant.now());

            // 3. Cache the response
            cacheResponse(cacheKey, response);

            return response;
        } catch (Exception e) {
            log.warn("Credit Bureau: CIBIL API failed after retries for PAN {}***: {}",
                    pan.substring(0, 3), e.getMessage());

            // 4. Fallback to simulated data
            return buildSimulatedResponse(pan);
        }
    }

    /**
     * Pull report with retry (exponential backoff).
     * 3 attempts, delays: 1s → 2s → 4s.
     */
    CreditBureauResponse pullWithRetry(CreditBureauRequest request) {
        int maxRetries = properties.getMaxRetries();
        long delayMs = properties.getRetryDelayMs();
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return cibilApiClient.fetchCreditReport(request);
            } catch (Exception e) {
                lastException = e;
                log.warn("Credit Bureau: Attempt {}/{} failed: {}",
                        attempt + 1, maxRetries, e.getMessage());

                if (attempt < maxRetries - 1) {
                    try {
                        long sleepMs = delayMs * (long) Math.pow(2, attempt);
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Credit bureau pull interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException("CIBIL API failed after " + maxRetries + " attempts", lastException);
    }

    /**
     * Generate simulated fallback data.
     * Uses conservative defaults matching the previous DecisionFactMapper stubs.
     */
    CreditBureauResponse buildSimulatedResponse(String pan) {
        log.info("Credit Bureau: Using SIMULATED data for PAN {}***", pan.substring(0, 3));
        return CreditBureauResponse.builder()
                .pan(pan)
                .creditScore(700)       // Same default as original DecisionFactMapper
                .scoreVersion("Simulated")
                .scoreFactors(new ArrayList<>())
                .accounts(new ArrayList<>())
                .enquiries(new ArrayList<>())
                .dpd90PlusCount(0)
                .writtenOffAccounts(0)
                .enquiryCount30Days(1)
                .totalActiveAccounts(0)
                .totalOutstandingBalance(0)
                .dataSource(BureauDataSource.SIMULATED)
                .pullTimestamp(Instant.now())
                .controlNumber("SIM-FALLBACK")
                .build();
    }

    private CreditBureauResponse getCachedResponse(String cacheKey) {
        try {
            Object cached = creditBureauRedisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof CreditBureauResponse response) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Credit Bureau: Redis cache read failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheResponse(String cacheKey, CreditBureauResponse response) {
        try {
            creditBureauRedisTemplate.opsForValue().set(
                    cacheKey,
                    response,
                    Duration.ofHours(properties.getCacheTtlHours()));
            log.debug("Credit Bureau: Cached response for key {}", cacheKey);
        } catch (Exception e) {
            log.warn("Credit Bureau: Redis cache write failed: {}", e.getMessage());
            // Non-fatal — continue without caching
        }
    }
}
