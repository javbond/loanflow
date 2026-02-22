package com.loanflow.loan.creditbureau;

import com.loanflow.loan.creditbureau.dto.BureauDataSource;
import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.creditbureau.service.CreditBureauService;
import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationRequest;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationResponse;
import com.loanflow.loan.incomeverification.dto.IncomeDataSource;
import com.loanflow.loan.incomeverification.service.IncomeVerificationService;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.workflow.delegate.CreditCheckDelegate;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for CreditCheckDelegate's integration with CreditBureauService.
 * Verifies that the delegate correctly pulls bureau data and passes it
 * to the decision engine.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditCheckDelegate — Bureau Integration")
class CreditCheckDelegateBureauTest {

    @Mock private LoanApplicationRepository repository;
    @Mock private DecisionEngineService decisionEngineService;
    @Mock private CreditBureauService creditBureauService;
    @Mock private IncomeVerificationService incomeVerificationService;
    @Mock private DelegateExecution execution;

    @InjectMocks
    private CreditCheckDelegate delegate;

    private static final String TEST_PAN = "ABCDE1234F";

    @Test
    @DisplayName("Should pull bureau report and pass to decision engine when PAN available")
    void shouldPullBureauAndPassToDecisionEngine() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId, LoanStatus.DOCUMENT_VERIFICATION);

        CreditBureauResponse bureauResponse = buildBureauResponse(780, BureauDataSource.REAL);
        IncomeVerificationResponse incomeResponse = buildIncomeResponse();
        DecisionResult result = buildApprovedResult(780);

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn(TEST_PAN);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(creditBureauService.pullReport(any(CreditBureauRequest.class))).thenReturn(bureauResponse);
        when(incomeVerificationService.verify(any(IncomeVerificationRequest.class))).thenReturn(incomeResponse);
        when(decisionEngineService.evaluate(eq(app), eq(bureauResponse), eq(incomeResponse))).thenReturn(result);

        delegate.execute(execution);

        // Verify bureau service was called with correct PAN
        ArgumentCaptor<CreditBureauRequest> captor = ArgumentCaptor.forClass(CreditBureauRequest.class);
        verify(creditBureauService).pullReport(captor.capture());
        assertThat(captor.getValue().getPan()).isEqualTo(TEST_PAN);

        // Verify decision engine used 3-arg overload (bureau + income)
        verify(decisionEngineService).evaluate(app, bureauResponse, incomeResponse);
        verify(decisionEngineService, never()).evaluate(app);

        // Verify bureau metadata persisted
        assertThat(app.getBureauDataSource()).isEqualTo("REAL");
        assertThat(app.getBureauPullTimestamp()).isNotNull();

        // Verify income metadata persisted
        assertThat(app.getIncomeVerified()).isTrue();
        assertThat(app.getIncomeDataSource()).isEqualTo("SIMULATED");

        // Verify process variables set
        verify(execution).setVariable("bureauDataSource", "REAL");
        verify(execution).setVariable("bureauControlNumber", "MOCK-TEST");
    }

    @Test
    @DisplayName("Should fallback to default evaluate when no PAN in process variables")
    void shouldFallbackWhenNoPan() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId, LoanStatus.DOCUMENT_VERIFICATION);

        DecisionResult result = buildApprovedResult(700);

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn(null);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(decisionEngineService.evaluate(app)).thenReturn(result);

        delegate.execute(execution);

        // Verify bureau service was NOT called
        verifyNoInteractions(creditBureauService);

        // Verify old evaluate path used
        verify(decisionEngineService).evaluate(app);
        verify(decisionEngineService, never()).evaluate(any(LoanApplication.class), any(CreditBureauResponse.class));

        // Bureau metadata should NOT be set
        assertThat(app.getBureauDataSource()).isNull();
    }

    @Test
    @DisplayName("Should handle CACHED bureau response correctly")
    void shouldHandleCachedResponse() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId, LoanStatus.DOCUMENT_VERIFICATION);

        CreditBureauResponse cachedResponse = buildBureauResponse(720, BureauDataSource.CACHED);
        IncomeVerificationResponse incomeResponse = buildIncomeResponse();
        DecisionResult result = buildApprovedResult(720);

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn(TEST_PAN);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(creditBureauService.pullReport(any())).thenReturn(cachedResponse);
        when(incomeVerificationService.verify(any(IncomeVerificationRequest.class))).thenReturn(incomeResponse);
        when(decisionEngineService.evaluate(eq(app), eq(cachedResponse), eq(incomeResponse))).thenReturn(result);

        delegate.execute(execution);

        assertThat(app.getBureauDataSource()).isEqualTo("CACHED");
        verify(execution).setVariable("bureauDataSource", "CACHED");
    }

    @Test
    @DisplayName("Should handle SIMULATED bureau fallback correctly")
    void shouldHandleSimulatedFallback() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId, LoanStatus.DOCUMENT_VERIFICATION);

        CreditBureauResponse simulatedResponse = buildBureauResponse(700, BureauDataSource.SIMULATED);
        simulatedResponse.setControlNumber("SIM-FALLBACK");
        IncomeVerificationResponse incomeResponse = buildIncomeResponse();
        DecisionResult result = buildApprovedResult(700);

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn(TEST_PAN);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(creditBureauService.pullReport(any())).thenReturn(simulatedResponse);
        when(incomeVerificationService.verify(any(IncomeVerificationRequest.class))).thenReturn(incomeResponse);
        when(decisionEngineService.evaluate(eq(app), eq(simulatedResponse), eq(incomeResponse))).thenReturn(result);

        delegate.execute(execution);

        assertThat(app.getBureauDataSource()).isEqualTo("SIMULATED");
        verify(execution).setVariable("bureauDataSource", "SIMULATED");
        verify(execution).setVariable("bureauControlNumber", "SIM-FALLBACK");
    }

    // ===== Helpers =====

    private static LoanApplication buildApplication(UUID id, LoanStatus status) {
        return LoanApplication.builder()
                .id(id)
                .applicationNumber("LN-2024-000001")
                .customerId(UUID.randomUUID())
                .customerEmail("test@example.com")
                .loanType(LoanType.PERSONAL_LOAN)
                .requestedAmount(new BigDecimal("500000"))
                .tenureMonths(36)
                .status(status)
                .build();
    }

    private static CreditBureauResponse buildBureauResponse(int score, BureauDataSource source) {
        return CreditBureauResponse.builder()
                .pan(TEST_PAN)
                .creditScore(score)
                .scoreVersion("Mock v1.0")
                .scoreFactors(new ArrayList<>())
                .accounts(new ArrayList<>())
                .enquiries(new ArrayList<>())
                .dpd90PlusCount(0)
                .writtenOffAccounts(0)
                .enquiryCount30Days(1)
                .totalActiveAccounts(2)
                .totalOutstandingBalance(100000)
                .dataSource(source)
                .pullTimestamp(Instant.now())
                .controlNumber("MOCK-TEST")
                .build();
    }

    private static IncomeVerificationResponse buildIncomeResponse() {
        return IncomeVerificationResponse.builder()
                .pan(TEST_PAN)
                .incomeVerified(true)
                .verifiedMonthlyIncome(new BigDecimal("75000"))
                .dtiRatio(new BigDecimal("0.35"))
                .incomeConsistencyScore(85)
                .flags(new ArrayList<>())
                .dataSource(IncomeDataSource.SIMULATED)
                .verificationTimestamp(Instant.now())
                .build();
    }

    private static DecisionResult buildApprovedResult(int creditScore) {
        return DecisionResult.builder()
                .eligible(true)
                .eligibilityStatus("ELIGIBLE")
                .creditScore(creditScore)
                .riskCategory("LOW")
                .riskTier("A")
                .interestRate(10.0)
                .processingFee(10000)
                .decision("APPROVED")
                .rulesFired(15)
                .rejectionReasons(List.of())
                .build();
    }
}
