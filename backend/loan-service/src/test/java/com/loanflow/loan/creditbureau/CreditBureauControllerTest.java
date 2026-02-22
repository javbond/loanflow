package com.loanflow.loan.creditbureau;

import com.loanflow.loan.creditbureau.controller.CreditBureauController;
import com.loanflow.loan.creditbureau.dto.BureauDataSource;
import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.creditbureau.dto.CreditPullRequest;
import com.loanflow.loan.creditbureau.service.CreditBureauService;
import com.loanflow.loan.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreditBureauController.
 * Uses plain Mockito (no Spring context) for fast, isolated testing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Credit Bureau Controller Tests")
class CreditBureauControllerTest {

    @Mock
    private CreditBureauService creditBureauService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @InjectMocks
    private CreditBureauController controller;

    private static final String TEST_PAN = "ABCDE1234F";
    private CreditBureauResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = CreditBureauResponse.builder()
                .pan(TEST_PAN)
                .creditScore(750)
                .scoreVersion("Mock v1.0")
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
                .controlNumber("MOCK-001")
                .build();
    }

    @Test
    @DisplayName("pullReport should return credit bureau report for valid PAN")
    void shouldReturnReportForValidPan() {
        CreditPullRequest request = CreditPullRequest.builder()
                .pan(TEST_PAN)
                .build();

        when(creditBureauService.pullReport(any(CreditBureauRequest.class))).thenReturn(mockResponse);

        ResponseEntity<CreditBureauResponse> response = controller.pullReport(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCreditScore()).isEqualTo(750);
        assertThat(response.getBody().getPan()).isEqualTo(TEST_PAN);
        assertThat(response.getBody().getDataSource()).isEqualTo(BureauDataSource.REAL);
        verify(creditBureauService).pullReport(any(CreditBureauRequest.class));
    }

    @Test
    @DisplayName("getCachedReport should return report by PAN")
    void shouldReturnCachedReport() {
        mockResponse.setDataSource(BureauDataSource.CACHED);
        when(creditBureauService.pullReport(any(CreditBureauRequest.class))).thenReturn(mockResponse);

        ResponseEntity<CreditBureauResponse> response = controller.getCachedReport(TEST_PAN);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDataSource()).isEqualTo(BureauDataSource.CACHED);
    }

    @Test
    @DisplayName("pullReport should throw when PAN is missing")
    void shouldThrowWhenPanMissing() {
        CreditPullRequest request = CreditPullRequest.builder().build();

        assertThatThrownBy(() -> controller.pullReport(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAN is required");
    }
}
