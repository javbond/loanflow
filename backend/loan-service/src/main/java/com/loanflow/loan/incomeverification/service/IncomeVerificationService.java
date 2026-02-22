package com.loanflow.loan.incomeverification.service;

import com.loanflow.loan.incomeverification.client.IncomeVerificationApiClient;
import com.loanflow.loan.incomeverification.config.IncomeVerificationProperties;
import com.loanflow.loan.incomeverification.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Core income verification service — orchestrates ITR/GST/bank verification with caching and fallback.
 *
 * Flow:
 * 1. Check Redis cache by PAN
 * 2. If hit → return with dataSource=CACHED
 * 3. If miss → call verification API with retry
 * 4. On success → cache in Redis with TTL, return with dataSource=REAL
 * 5. On failure → return simulated fallback with dataSource=SIMULATED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeVerificationService {

    private final IncomeVerificationApiClient apiClient;
    private final RedisTemplate<String, Object> incomeVerificationRedisTemplate;
    private final IncomeVerificationProperties properties;

    private static final String CACHE_KEY_PREFIX = "INCOME:";

    /**
     * Verify income with cache-first strategy.
     */
    public IncomeVerificationResponse verify(IncomeVerificationRequest request) {
        String pan = request.getPan();
        String cacheKey = CACHE_KEY_PREFIX + pan;

        // 1. Check Redis cache
        IncomeVerificationResponse cached = getCachedResponse(cacheKey);
        if (cached != null) {
            log.info("Income Verification: Cache HIT for PAN {}***", pan.substring(0, 3));
            cached.setDataSource(IncomeDataSource.CACHED);
            return cached;
        }

        // 2. Cache miss — call verification API with retry
        log.info("Income Verification: Cache MISS for PAN {}***, calling verification API", pan.substring(0, 3));
        try {
            IncomeVerificationResponse response = verifyWithRetry(request);
            response.setDataSource(IncomeDataSource.REAL);
            response.setVerificationTimestamp(Instant.now());

            // 3. Cache the response
            cacheResponse(cacheKey, response);

            return response;
        } catch (Exception e) {
            log.warn("Income Verification: API failed after retries for PAN {}***: {}",
                    pan.substring(0, 3), e.getMessage());

            // 4. Fallback to simulated data
            return buildSimulatedResponse(pan, request.getDeclaredMonthlyIncome());
        }
    }

    /**
     * Verify with retry (exponential backoff).
     */
    IncomeVerificationResponse verifyWithRetry(IncomeVerificationRequest request) {
        int maxRetries = properties.getMaxRetries();
        long delayMs = properties.getRetryDelayMs();
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return apiClient.verifyIncome(request);
            } catch (Exception e) {
                lastException = e;
                log.warn("Income Verification: Attempt {}/{} failed: {}",
                        attempt + 1, maxRetries, e.getMessage());

                if (attempt < maxRetries - 1) {
                    try {
                        long sleepMs = delayMs * (long) Math.pow(2, attempt);
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Income verification interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Income verification API failed after " + maxRetries + " attempts", lastException);
    }

    /**
     * Generate simulated fallback data.
     * Uses conservative defaults — income not verified, neutral DTI.
     */
    IncomeVerificationResponse buildSimulatedResponse(String pan, BigDecimal declaredIncome) {
        log.info("Income Verification: Using SIMULATED data for PAN {}***", pan.substring(0, 3));

        BigDecimal income = declaredIncome != null ? declaredIncome : BigDecimal.valueOf(50000);

        return IncomeVerificationResponse.builder()
                .pan(pan)
                .incomeVerified(false)
                .verifiedMonthlyIncome(income)
                .dtiRatio(BigDecimal.valueOf(0.3))    // Conservative 30% DTI
                .incomeConsistencyScore(50)             // Neutral consistency
                .itrData(null)
                .gstData(null)
                .bankStatementData(null)
                .flags(new ArrayList<>(java.util.List.of("SIMULATED: Income data unavailable, using declared values")))
                .dataSource(IncomeDataSource.SIMULATED)
                .verificationTimestamp(Instant.now())
                .build();
    }

    private IncomeVerificationResponse getCachedResponse(String cacheKey) {
        try {
            Object cached = incomeVerificationRedisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof IncomeVerificationResponse response) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Income Verification: Redis cache read failed: {}", e.getMessage());
        }
        return null;
    }

    private void cacheResponse(String cacheKey, IncomeVerificationResponse response) {
        try {
            incomeVerificationRedisTemplate.opsForValue().set(
                    cacheKey,
                    response,
                    Duration.ofHours(properties.getCacheTtlHours()));
            log.debug("Income Verification: Cached response for key {}", cacheKey);
        } catch (Exception e) {
            log.warn("Income Verification: Redis cache write failed: {}", e.getMessage());
            // Non-fatal — continue without caching
        }
    }
}
