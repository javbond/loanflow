package com.loanflow.loan.creditbureau;

import com.loanflow.loan.creditbureau.client.CibilApiClient;
import com.loanflow.loan.creditbureau.config.CibilProperties;
import com.loanflow.loan.creditbureau.dto.BureauDataSource;
import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.creditbureau.service.CreditBureauService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Credit Bureau Service — Cache & Fallback")
class CreditBureauServiceTest {

    @Mock private CibilApiClient cibilApiClient;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private CibilProperties properties;
    private CreditBureauService service;

    private static final String TEST_PAN = "ABCDE1234F";
    private static final String CACHE_KEY = "CIBIL:ABCDE1234F";

    @BeforeEach
    void setUp() {
        properties = new CibilProperties();
        properties.setMaxRetries(3);
        properties.setRetryDelayMs(10);  // Short delay for tests
        properties.setCacheTtlHours(24);
        properties.setEnabled(true);

        service = new CreditBureauService(cibilApiClient, redisTemplate, properties);
    }

    private CreditBureauRequest buildRequest() {
        return CreditBureauRequest.builder().pan(TEST_PAN).build();
    }

    private CreditBureauResponse buildMockResponse(int score) {
        return CreditBureauResponse.builder()
                .pan(TEST_PAN)
                .creditScore(score)
                .scoreVersion("Test")
                .scoreFactors(new ArrayList<>())
                .accounts(new ArrayList<>())
                .enquiries(new ArrayList<>())
                .dpd90PlusCount(0)
                .writtenOffAccounts(0)
                .enquiryCount30Days(1)
                .totalActiveAccounts(2)
                .totalOutstandingBalance(100000)
                .dataSource(BureauDataSource.REAL)
                .pullTimestamp(Instant.now())
                .controlNumber("TEST-001")
                .build();
    }

    @Nested
    @DisplayName("Cache Hit Scenarios")
    class CacheHitTests {

        @Test
        @DisplayName("Should return cached response when cache hit")
        void shouldReturnCachedResponse() {
            CreditBureauResponse cached = buildMockResponse(750);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(cached);

            CreditBureauResponse result = service.pullReport(buildRequest());

            assertThat(result.getCreditScore()).isEqualTo(750);
            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.CACHED);
            verifyNoInteractions(cibilApiClient);
        }

        @Test
        @DisplayName("Should set dataSource to CACHED when from cache")
        void shouldSetDataSourceCached() {
            CreditBureauResponse cached = buildMockResponse(680);
            cached.setDataSource(BureauDataSource.REAL);  // Original was REAL
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(cached);

            CreditBureauResponse result = service.pullReport(buildRequest());
            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.CACHED);
        }
    }

    @Nested
    @DisplayName("Cache Miss — API Call Scenarios")
    class CacheMissTests {

        @Test
        @DisplayName("Should call API and cache result when cache miss")
        void shouldCallApiAndCacheOnMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(cibilApiClient.fetchCreditReport(any())).thenReturn(buildMockResponse(720));

            CreditBureauResponse result = service.pullReport(buildRequest());

            assertThat(result.getCreditScore()).isEqualTo(720);
            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.REAL);
            verify(cibilApiClient, times(1)).fetchCreditReport(any());
            verify(valueOperations).set(eq(CACHE_KEY), any(), any());
        }

        @Test
        @DisplayName("Should set dataSource to REAL when from fresh API call")
        void shouldSetDataSourceReal() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(cibilApiClient.fetchCreditReport(any())).thenReturn(buildMockResponse(780));

            CreditBureauResponse result = service.pullReport(buildRequest());
            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.REAL);
        }
    }

    @Nested
    @DisplayName("Fallback Scenarios")
    class FallbackTests {

        @Test
        @DisplayName("Should return simulated data when API fails and no cache")
        void shouldFallbackToSimulated() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(cibilApiClient.fetchCreditReport(any()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            CreditBureauResponse result = service.pullReport(buildRequest());

            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.SIMULATED);
            assertThat(result.getCreditScore()).isEqualTo(700);  // Default
        }

        @Test
        @DisplayName("Should use default values for simulated fallback")
        void shouldUseDefaultValuesForSimulated() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(cibilApiClient.fetchCreditReport(any()))
                    .thenThrow(new RuntimeException("Connection refused"));

            CreditBureauResponse result = service.pullReport(buildRequest());

            assertThat(result.getCreditScore()).isEqualTo(700);
            assertThat(result.getDpd90PlusCount()).isZero();
            assertThat(result.getWrittenOffAccounts()).isZero();
            assertThat(result.getEnquiryCount30Days()).isEqualTo(1);
            assertThat(result.getControlNumber()).isEqualTo("SIM-FALLBACK");
        }

        @Test
        @DisplayName("Should handle Redis failure gracefully")
        void shouldHandleRedisFailure() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenThrow(new RuntimeException("Redis down"));
            when(cibilApiClient.fetchCreditReport(any())).thenReturn(buildMockResponse(700));

            // Should proceed to API call despite Redis failure
            CreditBureauResponse result = service.pullReport(buildRequest());
            assertThat(result.getCreditScore()).isEqualTo(700);
            verify(cibilApiClient).fetchCreditReport(any());
        }
    }

    @Nested
    @DisplayName("Retry Scenarios")
    class RetryTests {

        @Test
        @DisplayName("Should retry 3 times on transient failure then fallback")
        void shouldRetry3Times() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(cibilApiClient.fetchCreditReport(any()))
                    .thenThrow(new RuntimeException("Timeout"));

            CreditBureauResponse result = service.pullReport(buildRequest());

            verify(cibilApiClient, times(3)).fetchCreditReport(any());
            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.SIMULATED);
        }

        @Test
        @DisplayName("Should succeed on second retry")
        void shouldSucceedOnSecondRetry() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(cibilApiClient.fetchCreditReport(any()))
                    .thenThrow(new RuntimeException("Timeout"))
                    .thenReturn(buildMockResponse(760));

            CreditBureauResponse result = service.pullReport(buildRequest());

            verify(cibilApiClient, times(2)).fetchCreditReport(any());
            assertThat(result.getCreditScore()).isEqualTo(760);
            assertThat(result.getDataSource()).isEqualTo(BureauDataSource.REAL);
        }
    }
}
