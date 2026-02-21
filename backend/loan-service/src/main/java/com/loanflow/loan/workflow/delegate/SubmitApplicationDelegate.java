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
 * Service task delegate: runs automatically after process start.
 * Transitions the loan application from SUBMITTED to DOCUMENT_VERIFICATION.
 */
@Component("submitApplicationDelegate")
@RequiredArgsConstructor
@Slf4j
public class SubmitApplicationDelegate implements JavaDelegate {

    private final LoanApplicationRepository repository;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [Submit]: Processing application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Transition to DOCUMENT_VERIFICATION
        application.transitionTo(LoanStatus.DOCUMENT_VERIFICATION);
        repository.save(application);

        log.info("Workflow [Submit]: Application {} transitioned to DOCUMENT_VERIFICATION",
                application.getApplicationNumber());
    }
}
