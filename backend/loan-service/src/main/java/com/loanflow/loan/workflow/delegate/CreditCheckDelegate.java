package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.creditbureau.service.CreditBureauService;
import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationRequest;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationResponse;
import com.loanflow.loan.incomeverification.service.IncomeVerificationService;
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
    private final CreditBureauService creditBureauService;
    private final IncomeVerificationService incomeVerificationService;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [CreditCheck]: Running Drools decision engine for application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Transition to CREDIT_CHECK
        application.transitionTo(LoanStatus.CREDIT_CHECK);

        // 1. Pull credit bureau report (cache-first with retry + fallback)
        String customerPan = (String) execution.getVariable("customerPan");
        CreditBureauResponse bureauResponse = null;
        if (customerPan != null && !customerPan.isBlank()) {
            log.info("Workflow [CreditCheck]: Pulling credit bureau report for PAN {}***",
                    customerPan.substring(0, 3));
            CreditBureauRequest bureauRequest = CreditBureauRequest.builder()
                    .pan(customerPan)
                    .build();
            bureauResponse = creditBureauService.pullReport(bureauRequest);

            // Persist bureau metadata on the application
            application.setBureauDataSource(bureauResponse.getDataSource().name());
            application.setBureauPullTimestamp(bureauResponse.getPullTimestamp());

            log.info("Workflow [CreditCheck]: Bureau report — score={}, source={}, controlNo={}",
                    bureauResponse.getCreditScore(),
                    bureauResponse.getDataSource(),
                    bureauResponse.getControlNumber());
        } else {
            log.warn("Workflow [CreditCheck]: No customerPan in process variables — using default bureau data");
        }

        // 2. Income verification (ITR, GST, bank statement analysis)
        IncomeVerificationResponse incomeResponse = null;
        if (customerPan != null && !customerPan.isBlank()) {
            log.info("Workflow [CreditCheck]: Running income verification for PAN {}***",
                    customerPan.substring(0, 3));
            String employmentType = (String) execution.getVariable("employmentType");
            String declaredIncomeStr = (String) execution.getVariable("declaredMonthlyIncome");
            BigDecimal declaredIncome = declaredIncomeStr != null
                    ? new BigDecimal(declaredIncomeStr) : null;

            IncomeVerificationRequest incomeRequest = IncomeVerificationRequest.builder()
                    .pan(customerPan)
                    .employmentType(employmentType)
                    .declaredMonthlyIncome(declaredIncome)
                    .build();
            incomeResponse = incomeVerificationService.verify(incomeRequest);

            // Persist income verification metadata on the application
            application.setIncomeVerified(incomeResponse.isIncomeVerified());
            application.setVerifiedMonthlyIncome(incomeResponse.getVerifiedMonthlyIncome());
            application.setDtiRatio(incomeResponse.getDtiRatio());
            application.setIncomeDataSource(incomeResponse.getDataSource().name());

            log.info("Workflow [CreditCheck]: Income verification — verified={}, income={}, DTI={}, source={}",
                    incomeResponse.isIncomeVerified(),
                    incomeResponse.getVerifiedMonthlyIncome(),
                    incomeResponse.getDtiRatio(),
                    incomeResponse.getDataSource());
        }

        // 3. Execute Drools decision engine (eligibility + pricing rules)
        DecisionResult result;
        if (bureauResponse != null && incomeResponse != null) {
            result = decisionEngineService.evaluate(application, bureauResponse, incomeResponse);
        } else if (bureauResponse != null) {
            result = decisionEngineService.evaluate(application, bureauResponse);
        } else {
            result = decisionEngineService.evaluate(application);
        }

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

        // Store bureau metadata in process variables
        if (bureauResponse != null) {
            execution.setVariable("bureauDataSource", bureauResponse.getDataSource().name());
            execution.setVariable("bureauControlNumber", bureauResponse.getControlNumber());
        }

        // Store income verification metadata in process variables
        if (incomeResponse != null) {
            execution.setVariable("incomeVerified", incomeResponse.isIncomeVerified());
            execution.setVariable("verifiedMonthlyIncome", incomeResponse.getVerifiedMonthlyIncome().toString());
            execution.setVariable("dtiRatio", incomeResponse.getDtiRatio().toString());
            execution.setVariable("incomeDataSource", incomeResponse.getDataSource().name());
            execution.setVariable("incomeConsistencyScore", incomeResponse.getIncomeConsistencyScore());
        }

        // Transition to UNDERWRITING after credit check
        application.transitionTo(LoanStatus.UNDERWRITING);
        repository.save(application);

        log.info("Workflow [CreditCheck]: Application {} — Decision={}, CIBIL={}, Risk={}, Rate={}, Rules={}, Bureau={}, Income={}",
                application.getApplicationNumber(),
                result.decision(),
                result.creditScore(),
                result.riskCategory(),
                result.interestRate(),
                result.rulesFired(),
                bureauResponse != null ? bureauResponse.getDataSource() : "N/A",
                incomeResponse != null ? incomeResponse.getDataSource() : "N/A");
    }
}
