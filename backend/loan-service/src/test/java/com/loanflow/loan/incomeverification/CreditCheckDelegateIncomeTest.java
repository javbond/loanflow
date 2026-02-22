package com.loanflow.loan.incomeverification;

import com.loanflow.loan.creditbureau.dto.BureauDataSource;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.creditbureau.service.CreditBureauService;
import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.incomeverification.dto.IncomeDataSource;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationResponse;
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
import static org.mockito.Mockito.*;

/**
 * Tests for income verification integration in CreditCheckDelegate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditCheckDelegate — Income Verification Integration")
class CreditCheckDelegateIncomeTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private DecisionEngineService decisionEngineService;

    @Mock
    private CreditBureauService creditBureauService;

    @Mock
    private IncomeVerificationService incomeVerificationService;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private CreditCheckDelegate delegate;

    @Test
    @DisplayName("Should call income verification when PAN is present")
    void shouldCallIncomeVerificationWithPan() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId);

        CreditBureauResponse bureauResponse = buildBureauResponse();
        IncomeVerificationResponse incomeResponse = buildIncomeResponse();
        DecisionResult result = buildDecisionResult();

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn("ABCDE1234F");
        when(execution.getVariable("employmentType")).thenReturn("SALARIED");
        when(execution.getVariable("declaredMonthlyIncome")).thenReturn("75000");
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(creditBureauService.pullReport(any())).thenReturn(bureauResponse);
        when(incomeVerificationService.verify(any())).thenReturn(incomeResponse);
        when(decisionEngineService.evaluate(any(LoanApplication.class),
                any(CreditBureauResponse.class), any(IncomeVerificationResponse.class)))
                .thenReturn(result);

        delegate.execute(execution);

        verify(incomeVerificationService).verify(any());
        verify(decisionEngineService).evaluate(any(LoanApplication.class),
                any(CreditBureauResponse.class), any(IncomeVerificationResponse.class));
    }

    @Test
    @DisplayName("Should skip income verification when PAN is absent")
    void shouldSkipIncomeVerificationWithoutPan() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId);
        DecisionResult result = buildDecisionResult();

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn(null);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(decisionEngineService.evaluate(app)).thenReturn(result);

        delegate.execute(execution);

        verify(incomeVerificationService, never()).verify(any());
        verify(decisionEngineService).evaluate(app); // 1-arg overload
    }

    @Test
    @DisplayName("Should persist income verification metadata on entity")
    void shouldPersistIncomeMetadata() {
        UUID appId = UUID.randomUUID();
        LoanApplication app = buildApplication(appId);

        CreditBureauResponse bureauResponse = buildBureauResponse();
        IncomeVerificationResponse incomeResponse = buildIncomeResponse();
        DecisionResult result = buildDecisionResult();

        when(execution.getVariable("applicationId")).thenReturn(appId.toString());
        when(execution.getVariable("customerPan")).thenReturn("ABCDE1234F");
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(creditBureauService.pullReport(any())).thenReturn(bureauResponse);
        when(incomeVerificationService.verify(any())).thenReturn(incomeResponse);
        when(decisionEngineService.evaluate(any(LoanApplication.class),
                any(CreditBureauResponse.class), any(IncomeVerificationResponse.class)))
                .thenReturn(result);

        delegate.execute(execution);

        assertThat(app.getIncomeVerified()).isTrue();
        assertThat(app.getVerifiedMonthlyIncome()).isEqualByComparingTo(BigDecimal.valueOf(72000));
        assertThat(app.getDtiRatio()).isEqualByComparingTo(BigDecimal.valueOf(0.35));
        assertThat(app.getIncomeDataSource()).isEqualTo("REAL");

        // Verify income process variables were stored
        verify(execution).setVariable("incomeVerified", true);
        verify(execution).setVariable("incomeDataSource", "REAL");
    }

    // ===== Helpers =====

    private LoanApplication buildApplication(UUID id) {
        return LoanApplication.builder()
                .id(id)
                .applicationNumber("LN-2024-000001")
                .customerId(UUID.randomUUID())
                .customerEmail("test@example.com")
                .loanType(LoanType.PERSONAL_LOAN)
                .requestedAmount(new BigDecimal("500000"))
                .tenureMonths(36)
                .status(LoanStatus.DOCUMENT_VERIFICATION)
                .build();
    }

    private CreditBureauResponse buildBureauResponse() {
        return CreditBureauResponse.builder()
                .pan("ABCDE1234F")
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

    private IncomeVerificationResponse buildIncomeResponse() {
        return IncomeVerificationResponse.builder()
                .pan("ABCDE1234F")
                .incomeVerified(true)
                .verifiedMonthlyIncome(BigDecimal.valueOf(72000))
                .dtiRatio(BigDecimal.valueOf(0.35))
                .incomeConsistencyScore(85)
                .flags(new ArrayList<>())
                .dataSource(IncomeDataSource.REAL)
                .verificationTimestamp(Instant.now())
                .build();
    }

    private DecisionResult buildDecisionResult() {
        return DecisionResult.builder()
                .eligible(true)
                .eligibilityStatus("ELIGIBLE")
                .creditScore(750)
                .riskCategory("LOW")
                .riskTier("A")
                .interestRate(10.0)
                .processingFee(10000)
                .decision("APPROVED")
                .rulesFired(18)
                .rejectionReasons(List.of())
                .build();
    }
}
