package com.loanflow.loan.incomeverification;

import com.loanflow.loan.incomeverification.client.IncomeVerificationApiClient;
import com.loanflow.loan.incomeverification.config.IncomeVerificationProperties;
import com.loanflow.loan.incomeverification.dto.*;
import com.loanflow.loan.incomeverification.service.IncomeVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Income Verification Service")
class IncomeVerificationServiceTest {

    @Mock
    private IncomeVerificationApiClient apiClient;

    @Mock
    private RedisTemplate<String, Object> incomeVerificationRedisTemplate;

    @Mock
    private IncomeVerificationProperties properties;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private IncomeVerificationService service;

    private IncomeVerificationRequest testRequest;
    private IncomeVerificationResponse mockResponse;

    @BeforeEach
    void setUp() {
        testRequest = IncomeVerificationRequest.builder()
                .pan("ABCDE1234F")
                .employmentType("SALARIED")
                .declaredMonthlyIncome(BigDecimal.valueOf(75000))
                .build();

        mockResponse = IncomeVerificationResponse.builder()
                .pan("ABCDE1234F")
                .incomeVerified(true)
                .verifiedMonthlyIncome(BigDecimal.valueOf(72000))
                .dtiRatio(BigDecimal.valueOf(0.35))
                .incomeConsistencyScore(85)
                .itrData(ItrData.builder()
                        .grossTotalIncome(BigDecimal.valueOf(864000))
                        .salaryIncome(BigDecimal.valueOf(864000))
                        .itrFormType("ITR-1")
                        .assessmentYear("2025-26")
                        .build())
                .bankStatementData(BankStatementData.builder()
                        .avgMonthlyBalance(BigDecimal.valueOf(150000))
                        .avgMonthlyCredits(BigDecimal.valueOf(72000))
                        .bounceCount(0)
                        .monthsAnalyzed(6)
                        .build())
                .flags(new ArrayList<>())
                .dataSource(IncomeDataSource.REAL)
                .verificationTimestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should return cached response when available")
    void shouldReturnCachedResponse() {
        when(incomeVerificationRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(mockResponse);

        IncomeVerificationResponse result = service.verify(testRequest);

        assertThat(result.getDataSource()).isEqualTo(IncomeDataSource.CACHED);
        verify(apiClient, never()).verifyIncome(any());
    }

    @Test
    @DisplayName("Should call API and cache on cache miss")
    void shouldCallApiOnCacheMiss() {
        when(incomeVerificationRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiClient.verifyIncome(any())).thenReturn(mockResponse);
        when(properties.getMaxRetries()).thenReturn(3);

        IncomeVerificationResponse result = service.verify(testRequest);

        assertThat(result.getDataSource()).isEqualTo(IncomeDataSource.REAL);
        verify(apiClient).verifyIncome(any());
        verify(valueOperations).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("Should return simulated response when API fails")
    void shouldReturnSimulatedOnApiFailure() {
        when(incomeVerificationRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiClient.verifyIncome(any())).thenThrow(new RuntimeException("API unavailable"));
        when(properties.getMaxRetries()).thenReturn(1);
        when(properties.getRetryDelayMs()).thenReturn(10L);

        IncomeVerificationResponse result = service.verify(testRequest);

        assertThat(result.getDataSource()).isEqualTo(IncomeDataSource.SIMULATED);
        assertThat(result.isIncomeVerified()).isFalse();
        assertThat(result.getFlags()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle Redis failure gracefully and call API")
    void shouldHandleRedisFailureGracefully() {
        when(incomeVerificationRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));
        when(apiClient.verifyIncome(any())).thenReturn(mockResponse);
        when(properties.getMaxRetries()).thenReturn(3);

        IncomeVerificationResponse result = service.verify(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.isIncomeVerified()).isTrue();
        verify(apiClient).verifyIncome(any());
    }
}
