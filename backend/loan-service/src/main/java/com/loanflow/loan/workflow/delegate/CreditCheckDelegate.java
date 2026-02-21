package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service task delegate: runs automatically after Document Verification is complete.
 * Executes Drools decision engine for credit assessment and pricing,
 * then transitions to UNDERWRITING.
 */
@Component("creditCheckDelegate")
@RequiredArgsConstructor
@Slf4j
public class CreditCheckDelegate implements JavaDelegate {

    private final LoanApplicationRepository repository;
    private final DecisionEngineService decisionEngineService;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [CreditCheck]: Running Drools decision engine for application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Transition to CREDIT_CHECK
        application.transitionTo(LoanStatus.CREDIT_CHECK);

        // Execute Drools decision engine (eligibility + pricing rules)
        DecisionResult result = decisionEngineService.evaluate(application);

        // Apply decision results to application
        application.setCibilScore(result.creditScore());
        application.setRiskCategory(result.riskCategory());

        if (result.interestRate() > 0) {
            application.setInterestRate(
                    BigDecimal.valueOf(result.interestRate()).setScale(2, RoundingMode.HALF_UP));
        }

        if (result.processingFee() > 0) {
            application.setProcessingFee(
                    BigDecimal.valueOf(result.processingFee()).setScale(2, RoundingMode.HALF_UP));
        }

        repository.save(application);

        // Store decision results in process variables for underwriting decision
        execution.setVariable("cibilScore", result.creditScore());
        execution.setVariable("riskCategory", result.riskCategory());
        execution.setVariable("riskTier", result.riskTier());
        execution.setVariable("eligibilityStatus", result.eligibilityStatus());
        execution.setVariable("interestRate", result.interestRate());
        execution.setVariable("processingFee", result.processingFee());
        execution.setVariable("rulesFired", result.rulesFired());
        execution.setVariable("decisionResult", result.decision());

        if (!result.rejectionReasons().isEmpty()) {
            execution.setVariable("rejectionReasons", String.join("; ", result.rejectionReasons()));
        }

        // Transition to UNDERWRITING after credit check
        application.transitionTo(LoanStatus.UNDERWRITING);
        repository.save(application);

        log.info("Workflow [CreditCheck]: Application {} — Decision={}, CIBIL={}, Risk={}, Rate={}, Rules={}",
                application.getApplicationNumber(),
                result.decision(),
                result.creditScore(),
                result.riskCategory(),
                result.interestRate(),
                result.rulesFired());
    }
}
