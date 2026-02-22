package com.loanflow.loan.incomeverification;

import com.loanflow.loan.incomeverification.controller.IncomeVerificationController;
import com.loanflow.loan.incomeverification.dto.*;
import com.loanflow.loan.incomeverification.service.IncomeVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IncomeVerificationController.
 * Uses plain Mockito (no Spring context) for fast, isolated testing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Income Verification Controller Tests")
class IncomeVerificationControllerTest {

    @Mock
    private IncomeVerificationService incomeVerificationService;

    @InjectMocks
    private IncomeVerificationController controller;

    private static final String TEST_PAN = "ABCDE1234F";
    private IncomeVerificationResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = IncomeVerificationResponse.builder()
                .pan(TEST_PAN)
                .incomeVerified(true)
                .verifiedMonthlyIncome(BigDecimal.valueOf(72000))
                .dtiRatio(BigDecimal.valueOf(0.35))
                .incomeConsistencyScore(85)
                .itrData(ItrData.builder()
                        .grossTotalIncome(BigDecimal.valueOf(864000))
                        .itrFormType("ITR-1")
                        .assessmentYear("2025-26")
                        .build())
                .flags(new ArrayList<>())
                .dataSource(IncomeDataSource.REAL)
                .verificationTimestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("verify should return income verification response for valid PAN")
    void shouldReturnResponseForValidPan() {
        IncomeVerificationRequest request = IncomeVerificationRequest.builder()
                .pan(TEST_PAN)
                .employmentType("SALARIED")
                .declaredMonthlyIncome(BigDecimal.valueOf(75000))
                .build();

        when(incomeVerificationService.verify(any(IncomeVerificationRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<IncomeVerificationResponse> response = controller.verify(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isIncomeVerified()).isTrue();
        assertThat(response.getBody().getVerifiedMonthlyIncome())
                .isEqualByComparingTo(BigDecimal.valueOf(72000));
        verify(incomeVerificationService).verify(any(IncomeVerificationRequest.class));
    }

    @Test
    @DisplayName("verify should throw when PAN is missing")
    void shouldThrowWhenPanMissing() {
        IncomeVerificationRequest request = IncomeVerificationRequest.builder().build();

        assertThatThrownBy(() -> controller.verify(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAN is required");
    }
}
