package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.creditbureau.service.CreditBureauService;
import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.incomeverification.service.IncomeVerificationService;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.LoanApplicationRepository;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Workflow Delegate Tests")
class DelegateTests {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("SubmitApplicationDelegate")
    class SubmitDelegateTests {

        @Mock
        private LoanApplicationRepository repository;

        @Mock
        private DelegateExecution execution;

        @InjectMocks
        private SubmitApplicationDelegate delegate;

        @Test
        @DisplayName("Should transition application to DOCUMENT_VERIFICATION")
        void shouldTransitionToDocumentVerification() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.SUBMITTED);

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.DOCUMENT_VERIFICATION);
            verify(repository).save(app);
        }

        @Test
        @DisplayName("Should throw when application not found")
        void shouldThrowWhenNotFound() {
            UUID appId = UUID.randomUUID();
            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(repository.findById(appId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> delegate.execute(execution))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("CreditCheckDelegate")
    class CreditCheckDelegateTests {

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
        @DisplayName("Should execute Drools decision engine and set results")
        void shouldExecuteDroolsDecisionEngine() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.DOCUMENT_VERIFICATION);

            DecisionResult result = DecisionResult.builder()
                    .eligible(true)
                    .eligibilityStatus("ELIGIBLE")
                    .creditScore(750)
                    .riskCategory("LOW")
                    .riskTier("A")
                    .interestRate(10.0)
                    .processingFee(10000)
                    .decision("APPROVED")
                    .rulesFired(15)
                    .rejectionReasons(List.of())
                    .build();

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(decisionEngineService.evaluate(app)).thenReturn(result);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.UNDERWRITING);
            assertThat(app.getCibilScore()).isEqualTo(750);
            assertThat(app.getRiskCategory()).isEqualTo("LOW");
            assertThat(app.getInterestRate()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(app.getProcessingFee()).isEqualByComparingTo(new BigDecimal("10000.00"));
            verify(execution).setVariable("cibilScore", 750);
            verify(execution).setVariable("riskCategory", "LOW");
            verify(execution).setVariable("riskTier", "A");
            verify(execution).setVariable("interestRate", 10.0);
            verify(execution).setVariable("rulesFired", 15);
            verify(repository, atLeast(2)).save(any());
        }

        @Test
        @DisplayName("Should handle rejected decision from Drools")
        void shouldHandleRejectedDecision() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.DOCUMENT_VERIFICATION);

            DecisionResult result = DecisionResult.builder()
                    .eligible(false)
                    .eligibilityStatus("REJECTED")
                    .creditScore(500)
                    .riskCategory("HIGH")
                    .riskTier("D")
                    .interestRate(0)
                    .processingFee(0)
                    .decision("REJECTED")
                    .rulesFired(5)
                    .rejectionReasons(List.of("Credit score below threshold"))
                    .build();

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(decisionEngineService.evaluate(app)).thenReturn(result);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.UNDERWRITING);
            assertThat(app.getCibilScore()).isEqualTo(500);
            assertThat(app.getRiskCategory()).isEqualTo("HIGH");
            verify(execution).setVariable("decisionResult", "REJECTED");
            verify(execution).setVariable("rejectionReasons", "Credit score below threshold");
        }

        @Test
        @DisplayName("Should throw when application not found")
        void shouldThrowWhenNotFound() {
            UUID appId = UUID.randomUUID();
            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(repository.findById(appId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> delegate.execute(execution))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("RejectionDelegate")
    class RejectionDelegateTests {

        @Mock
        private LoanApplicationRepository repository;

        @Mock
        private DelegateExecution execution;

        @InjectMocks
        private RejectionDelegate delegate;

        @Test
        @DisplayName("Should reject application with reason")
        void shouldRejectWithReason() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.UNDERWRITING);

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(execution.getVariable("comments")).thenReturn("Insufficient income");
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(app.getRejectionReason()).isEqualTo("Insufficient income");
            verify(repository).save(app);
        }

        @Test
        @DisplayName("Should use default reason when comments are empty")
        void shouldUseDefaultReason() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.UNDERWRITING);

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(execution.getVariable("comments")).thenReturn(null);
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.REJECTED);
            assertThat(app.getRejectionReason()).contains("underwriting review");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("ApprovalDelegate")
    class ApprovalDelegateTests {

        @Mock
        private LoanApplicationRepository repository;

        @Mock
        private DelegateExecution execution;

        @InjectMocks
        private ApprovalDelegate delegate;

        @Test
        @DisplayName("Should approve application with default rate")
        void shouldApproveWithDefaults() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.UNDERWRITING);

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(execution.getVariable("approvedAmount")).thenReturn(null);
            when(execution.getVariable("interestRate")).thenReturn(null);
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(app.getApprovedAmount()).isEqualTo(app.getRequestedAmount());
            assertThat(app.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should approve with custom amount and rate")
        void shouldApproveWithCustomValues() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = buildApplication(appId, LoanStatus.UNDERWRITING);

            when(execution.getVariable("applicationId")).thenReturn(appId.toString());
            when(execution.getVariable("approvedAmount")).thenReturn(new BigDecimal("400000"));
            when(execution.getVariable("interestRate")).thenReturn(new BigDecimal("10.5"));
            when(repository.findById(appId)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            delegate.execute(execution);

            assertThat(app.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(app.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("400000"));
            assertThat(app.getInterestRate()).isEqualByComparingTo(new BigDecimal("10.5"));
        }
    }

    // ===== Helper =====

    static LoanApplication buildApplication(UUID id, LoanStatus status) {
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
}
