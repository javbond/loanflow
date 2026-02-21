package com.loanflow.loan.workflow.listener;

import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.workflow.assignment.ApprovalHierarchyResolver;
import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.assignment.AssignmentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Task listener that auto-assigns workflow tasks to officers using the configured strategy.
 *
 * Fires on "create" event for each user task. For underwriting tasks, uses the
 * ApprovalHierarchyResolver to dynamically determine the correct candidate group
 * based on loan amount (approval matrix). For other tasks, uses the BPMN-defined
 * candidate group.
 *
 * Also syncs the selected officer to LoanApplication.assignedOfficer for entity-level tracking.
 *
 * Can be disabled via loanflow.assignment.enabled=false without modifying BPMN.
 */
@Component("autoAssignmentTaskListener")
@RequiredArgsConstructor
@Slf4j
public class AutoAssignmentTaskListener implements TaskListener {

    private final AssignmentProperties properties;
    private final AssignmentStrategy assignmentStrategy;
    private final LoanApplicationRepository repository;
    private final ApprovalHierarchyResolver hierarchyResolver;

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!properties.isEnabled()) {
            log.debug("Auto-assignment is disabled, skipping task {}", delegateTask.getId());
            return;
        }

        // Only act on task creation
        if (!"create".equals(delegateTask.getEventName())) {
            return;
        }

        // Extract candidate groups from the task
        Set<IdentityLink> candidates = delegateTask.getCandidates();
        if (candidates == null || candidates.isEmpty()) {
            log.debug("No candidate groups for task {}, skipping auto-assignment",
                    delegateTask.getTaskDefinitionKey());
            return;
        }

        // Use the first candidate group with a group ID (BPMN-defined default)
        Optional<String> bpmnCandidateGroup = candidates.stream()
                .filter(link -> link.getGroupId() != null)
                .map(IdentityLink::getGroupId)
                .findFirst();

        if (bpmnCandidateGroup.isEmpty()) {
            log.debug("No group-based candidates found for task {}", delegateTask.getTaskDefinitionKey());
            return;
        }

        // US-015: Resolve effective candidate group via approval hierarchy
        String applicationId = (String) delegateTask.getVariable("applicationId");
        String effectiveGroup = hierarchyResolver.resolveGroup(
                delegateTask.getTaskDefinitionKey(),
                applicationId,
                bpmnCandidateGroup.get());

        // If hierarchy resolved a different group, update the task's candidate group
        if (!effectiveGroup.equals(bpmnCandidateGroup.get())) {
            log.info("Approval hierarchy override: task={}, bpmn={} → resolved={}",
                    delegateTask.getTaskDefinitionKey(), bpmnCandidateGroup.get(), effectiveGroup);
            delegateTask.addCandidateGroup(effectiveGroup);
            delegateTask.deleteCandidateGroup(bpmnCandidateGroup.get());
        }

        // Select assignee via strategy using the effective group
        Optional<String> selectedAssignee = assignmentStrategy.selectAssignee(effectiveGroup);
        if (selectedAssignee.isEmpty()) {
            log.warn("No assignee selected for task {} (group={}), task remains unassigned",
                    delegateTask.getTaskDefinitionKey(), effectiveGroup);
            return;
        }

        String assigneeId = selectedAssignee.get();
        delegateTask.setAssignee(assigneeId);
        log.info("Auto-assigned task {} ({}) to user {} via group {}",
                delegateTask.getId(), delegateTask.getTaskDefinitionKey(),
                assigneeId, effectiveGroup);

        // Sync assignedOfficer on LoanApplication entity
        syncAssignedOfficer(delegateTask, assigneeId);
    }

    /**
     * Update LoanApplication.assignedOfficer to match the auto-assigned user.
     * Wrapped in try-catch to prevent assignment sync failures from blocking task creation.
     */
    private void syncAssignedOfficer(DelegateTask delegateTask, String assigneeId) {
        try {
            String applicationIdStr = (String) delegateTask.getVariable("applicationId");
            if (applicationIdStr == null) return;

            UUID applicationId = UUID.fromString(applicationIdStr);
            repository.findById(applicationId).ifPresent(application -> {
                application.setAssignedOfficer(UUID.fromString(assigneeId));
                repository.save(application);
                log.debug("Synced assignedOfficer={} for application {}", assigneeId, applicationId);
            });
        } catch (Exception e) {
            log.warn("Failed to sync assignedOfficer for task {}: {}",
                    delegateTask.getId(), e.getMessage());
        }
    }
}
