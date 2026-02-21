package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Service task delegate: runs automatically after Document Verification is complete.
 * Performs credit check (simulated) and transitions to UNDERWRITING.
 */
@Component("creditCheckDelegate")
@RequiredArgsConstructor
@Slf4j
public class CreditCheckDelegate implements JavaDelegate {

    private final LoanApplicationRepository repository;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [CreditCheck]: Running credit check for application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Transition to CREDIT_CHECK
        application.transitionTo(LoanStatus.CREDIT_CHECK);

        // Simulate credit check — in production, call external credit bureau (CIBIL, Experian, etc.)
        if (application.getCibilScore() == null) {
            application.setCibilScore(750);
            application.setRiskCategory("MEDIUM");
        }

        repository.save(application);

        // Store credit result in process variables for underwriting decision
        execution.setVariable("cibilScore", application.getCibilScore());
        execution.setVariable("riskCategory",
                application.getRiskCategory() != null ? application.getRiskCategory() : "MEDIUM");

        // Transition to UNDERWRITING after credit check
        application.transitionTo(LoanStatus.UNDERWRITING);
        repository.save(application);

        log.info("Workflow [CreditCheck]: Application {} credit check complete. CIBIL={}, Risk={}",
                application.getApplicationNumber(), application.getCibilScore(), application.getRiskCategory());
    }
}
