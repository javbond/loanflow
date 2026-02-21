package com.loanflow.loan.workflow.impl;

import com.loanflow.loan.workflow.WorkflowService;
import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.dto.OfficerWorkload;
import com.loanflow.loan.workflow.dto.TaskResponse;
import com.loanflow.loan.workflow.dto.WorkloadResponse;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private static final String PROCESS_KEY = "loanOrigination";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final AssignmentProperties assignmentProperties;

    @Override
    public String startProcess(UUID applicationId, Map<String, Object> variables) {
        variables.put("applicationId", applicationId.toString());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, applicationId.toString(), variables);

        log.info("Started workflow process {} for application {}", instance.getId(), applicationId);
        return instance.getId();
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        // Claim if not already claimed
        if (task.getAssignee() == null) {
            taskService.claim(taskId, userId);
        }

        taskService.complete(taskId, variables);
        log.info("User {} completed task {} ({}) with variables {}",
                userId, taskId, task.getName(), variables.keySet());
    }

    @Override
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
        log.info("User {} claimed task {}", userId, taskId);
    }

    @Override
    public void unclaimTask(String taskId) {
        taskService.unclaim(taskId);
        log.info("Task {} unclaimed", taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByRoles(List<String> roles, Pageable pageable) {
        TaskQuery query = taskService.createTaskQuery()
                .taskCandidateGroupIn(roles)
                .orderByTaskCreateTime().desc();

        long total = query.count();
        List<Task> tasks = query
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        List<TaskResponse> responses = tasks.stream()
                .map(this::mapToTaskResponse)
                .toList();

        return new PageImpl<>(responses, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByAssignee(String userId, Pageable pageable) {
        TaskQuery query = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc();

        long total = query.count();
        List<Task> tasks = query
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        List<TaskResponse> responses = tasks.stream()
                .map(this::mapToTaskResponse)
                .toList();

        return new PageImpl<>(responses, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("Task", "id", taskId);
        }
        return mapToTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getActiveTaskForApplication(UUID applicationId) {
        Task task = taskService.createTaskQuery()
                .processInstanceBusinessKey(applicationId.toString())
                .singleResult();
        if (task == null) {
            return null;
        }
        return mapToTaskResponse(task);
    }

    @Override
    public void cancelProcess(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        log.info("Cancelled workflow process {} — Reason: {}", processInstanceId, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkloadResponse getOfficerWorkload() {
        List<OfficerWorkload> workloads = new ArrayList<>();
        int totalActive = 0;

        for (Map.Entry<String, List<String>> entry : assignmentProperties.getOfficers().entrySet()) {
            String role = entry.getKey();
            for (String userId : entry.getValue()) {
                long activeCount = taskService.createTaskQuery()
                        .taskAssignee(userId)
                        .count();

                // Check if any assigned task is SLA-breached
                boolean slaBreached = false;
                AssignmentProperties.SlaConfig slaConfig = findSlaConfigForRole(role);
                if (slaConfig != null) {
                    Instant deadline = Instant.now().minus(slaConfig.getTimeoutHours(), ChronoUnit.HOURS);
                    long overdueCount = taskService.createTaskQuery()
                            .taskAssignee(userId)
                            .taskCreatedBefore(Date.from(deadline))
                            .count();
                    slaBreached = overdueCount > 0;
                }

                workloads.add(OfficerWorkload.builder()
                        .userId(userId)
                        .role(role)
                        .activeTaskCount(activeCount)
                        .slaBreached(slaBreached)
                        .build());

                totalActive += (int) activeCount;
            }
        }

        return WorkloadResponse.builder()
                .officers(workloads)
                .totalActiveTaskCount(totalActive)
                .build();
    }

    /**
     * Find SLA config for tasks typically assigned to this role.
     */
    private AssignmentProperties.SlaConfig findSlaConfigForRole(String role) {
        return switch (role) {
            case "LOAN_OFFICER" -> assignmentProperties.getSla().get("documentVerification");
            case "UNDERWRITER" -> assignmentProperties.getSla().get("underwritingReview");
            case "SENIOR_UNDERWRITER", "BRANCH_MANAGER" -> assignmentProperties.getSla().get("referredReview");
            default -> null;
        };
    }

    private TaskResponse mapToTaskResponse(Task task) {
        Map<String, Object> variables = taskService.getVariables(task.getId());

        List<String> candidateGroups = taskService.getIdentityLinksForTask(task.getId()).stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                .map(link -> link.getGroupId())
                .toList();

        return TaskResponse.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .candidateGroups(candidateGroups)
                .applicationId(getStringVar(variables, "applicationId"))
                .applicationNumber(getStringVar(variables, "applicationNumber"))
                .loanType(getStringVar(variables, "loanType"))
                .customerEmail(getStringVar(variables, "customerEmail"))
                .requestedAmount(getStringVar(variables, "requestedAmount"))
                .cibilScore(variables.get("cibilScore") instanceof Integer i ? i : null)
                .riskCategory(getStringVar(variables, "riskCategory"))
                .processInstanceId(task.getProcessInstanceId())
                .createdAt(task.getCreateTime() != null ? task.getCreateTime().toInstant() : null)
                .dueDate(task.getDueDate() != null ? task.getDueDate().toInstant() : null)
                .formKey(task.getFormKey())
                .build();
    }

    private String getStringVar(Map<String, Object> variables, String key) {
        Object val = variables.get(key);
        return val != null ? val.toString() : null;
    }
}
