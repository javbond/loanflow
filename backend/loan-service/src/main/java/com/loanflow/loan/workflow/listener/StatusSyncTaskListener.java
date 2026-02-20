package com.loanflow.loan.workflow.listener;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Task listener that syncs LoanApplication.status when a user task is created.
 * This ensures the entity status reflects the current BPMN process state.
 */
@Component("statusSyncTaskListener")
@RequiredArgsConstructor
@Slf4j
public class StatusSyncTaskListener implements TaskListener {

    private final LoanApplicationRepository repository;

    @Override
    public void notify(DelegateTask delegateTask) {
        String applicationId = (String) delegateTask.getVariable("applicationId");
        if (applicationId == null) return;

        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String eventName = delegateTask.getEventName();

        log.info("TaskSync: task={}, event={}, applicationId={}", taskDefinitionKey, eventName, applicationId);

        // Only sync status on task creation (not completion — delegates handle that)
        if (!"create".equals(eventName)) return;

        LoanApplication application = repository.findById(UUID.fromString(applicationId)).orElse(null);
        if (application == null) return;

        LoanStatus targetStatus = switch (taskDefinitionKey) {
            case "documentVerification" -> LoanStatus.DOCUMENT_VERIFICATION;
            case "underwritingReview" -> LoanStatus.UNDERWRITING;
            case "referredReview" -> LoanStatus.REFERRED;
            default -> null;
        };

        if (targetStatus != null && application.getStatus() != targetStatus) {
            log.info("TaskSync: Syncing application {} status from {} to {}",
                    applicationId, application.getStatus(), targetStatus);
            // Direct set instead of transitionTo() to avoid constraint errors
            // when the delegate already moved the status forward
            application.setStatus(targetStatus);
            repository.save(application);
        }
    }
}
