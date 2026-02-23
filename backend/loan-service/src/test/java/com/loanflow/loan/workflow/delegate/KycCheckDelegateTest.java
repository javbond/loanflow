package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for KycCheckDelegate (US-029).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KycCheckDelegate Tests")
class KycCheckDelegateTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private KycCheckDelegate delegate;

    private UUID applicationId;
    private UUID customerId;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        application = LoanApplication.builder()
                .id(applicationId)
                .applicationNumber("LN-2026-000001")
                .customerId(customerId)
                .build();

        ReflectionTestUtils.setField(delegate, "customerServiceUrl", "http://localhost:8082");
    }

    @Test
    @DisplayName("Should set kycVerified=true when KYC status is VERIFIED")
    void shouldSetKycVerifiedWhenStatusIsVerified() {
        when(execution.getVariable("applicationId")).thenReturn(applicationId.toString());
        when(repository.findById(applicationId)).thenReturn(Optional.of(application));

        Map<String, Object> data = Map.of("status", "VERIFIED", "ckycNumber", "CKYC-2026-00012345");
        Map<String, Object> apiResponse = Map.of("success", true, "data", data);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        delegate.execute(execution);

        verify(execution).setVariable("kycStatus", "VERIFIED");
        verify(execution).setVariable("kycVerified", true);
    }

    @Test
    @DisplayName("Should set kycVerified=false when KYC status is NOT_INITIATED")
    void shouldSetKycNotVerifiedWhenNotInitiated() {
        when(execution.getVariable("applicationId")).thenReturn(applicationId.toString());
        when(repository.findById(applicationId)).thenReturn(Optional.of(application));

        Map<String, Object> data = Map.of("status", "NOT_INITIATED");
        Map<String, Object> apiResponse = Map.of("success", true, "data", data);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        delegate.execute(execution);

        verify(execution).setVariable("kycStatus", "NOT_INITIATED");
        verify(execution).setVariable("kycVerified", false);
    }

    @Test
    @DisplayName("Should gracefully handle REST call failure and proceed")
    void shouldHandleRestCallFailureGracefully() {
        when(execution.getVariable("applicationId")).thenReturn(applicationId.toString());
        when(repository.findById(applicationId)).thenReturn(Optional.of(application));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        delegate.execute(execution);

        // Should still set variables with defaults and not throw
        verify(execution).setVariable("kycStatus", "UNKNOWN");
        verify(execution).setVariable("kycVerified", false);
    }
}
