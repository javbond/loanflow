package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Service task delegate: runs when the underwriting decision is REJECTED.
 * Rejects the loan application with the provided reason.
 */
@Component("rejectionDelegate")
@RequiredArgsConstructor
@Slf4j
public class RejectionDelegate implements JavaDelegate {

    private final LoanApplicationRepository repository;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [Rejection]: Processing rejection for application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        String reason = (String) execution.getVariable("comments");
        if (reason == null || reason.isBlank()) {
            reason = "Application rejected during underwriting review";
        }

        application.reject(reason);
        repository.save(application);

        log.info("Workflow [Rejection]: Application {} REJECTED. Reason: {}",
                application.getApplicationNumber(), reason);
    }
}
