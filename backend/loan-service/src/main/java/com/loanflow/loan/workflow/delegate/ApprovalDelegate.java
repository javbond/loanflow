package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service task delegate: runs when the underwriting decision is APPROVED.
 * Approves the loan application with the requested amount and a default interest rate.
 */
@Component("approvalDelegate")
@RequiredArgsConstructor
@Slf4j
public class ApprovalDelegate implements JavaDelegate {

    private final LoanApplicationRepository repository;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [Approval]: Processing approval for application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Use approved amount/rate from task variables if set, otherwise use defaults
        BigDecimal approvedAmount = getAmountVariable(execution, "approvedAmount",
                application.getRequestedAmount());
        BigDecimal interestRate = getAmountVariable(execution, "interestRate",
                BigDecimal.valueOf(application.getLoanType().getBaseInterestRate()));

        application.approve(approvedAmount, interestRate);
        repository.save(application);

        log.info("Workflow [Approval]: Application {} APPROVED. Amount={}, Rate={}%",
                application.getApplicationNumber(), approvedAmount, interestRate);
    }

    private BigDecimal getAmountVariable(DelegateExecution execution, String name, BigDecimal defaultValue) {
        Object value = execution.getVariable(name);
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
