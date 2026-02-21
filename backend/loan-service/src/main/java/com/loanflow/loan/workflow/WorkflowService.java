package com.loanflow.loan.workflow;

import com.loanflow.loan.workflow.dto.TaskResponse;
import com.loanflow.loan.workflow.dto.WorkloadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for Flowable BPMN workflow operations.
 */
public interface WorkflowService {

    /**
     * Start a new loan origination workflow process.
     *
     * @param applicationId the loan application UUID
     * @param variables     process variables (applicationNumber, loanType, etc.)
     * @return the Flowable process instance ID
     */
    String startProcess(UUID applicationId, Map<String, Object> variables);

    /**
     * Complete a user task with the given variables.
     *
     * @param taskId    the Flowable task ID
     * @param variables completion variables (e.g., decision = APPROVED/REJECTED/REFERRED)
     * @param userId    the user completing the task
     */
    void completeTask(String taskId, Map<String, Object> variables, String userId);

    /**
     * Claim a task for a specific user.
     */
    void claimTask(String taskId, String userId);

    /**
     * Release a claimed task back to the candidate group pool.
     */
    void unclaimTask(String taskId);

    /**
     * Get tasks available for the given roles (candidate group inbox).
     */
    Page<TaskResponse> getTasksByRoles(List<String> roles, Pageable pageable);

    /**
     * Get tasks claimed by a specific user.
     */
    Page<TaskResponse> getTasksByAssignee(String userId, Pageable pageable);

    /**
     * Get a specific task by its Flowable task ID.
     */
    TaskResponse getTask(String taskId);

    /**
     * Get the current active user task for a loan application.
     *
     * @return the active task, or null if no user task is active
     */
    TaskResponse getActiveTaskForApplication(UUID applicationId);

    /**
     * Cancel/delete a running process instance.
     */
    void cancelProcess(String processInstanceId, String reason);

    /**
     * Get workload summary for all configured officers.
     * Shows active task count and SLA breach status per officer.
     */
    WorkloadResponse getOfficerWorkload();
}
