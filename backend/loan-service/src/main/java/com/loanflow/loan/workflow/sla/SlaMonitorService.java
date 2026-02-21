package com.loanflow.loan.workflow.sla;

import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.assignment.AssignmentProperties.SlaConfig;
import com.loanflow.loan.workflow.assignment.AssignmentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * SLA monitoring service that periodically checks active workflow tasks
 * for SLA breaches and escalates overdue tasks to supervisor roles.
 *
 * Runs on a fixed schedule (default: every 5 minutes).
 * Can be disabled via loanflow.assignment.sla-enabled=false.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaMonitorService {

    private final TaskService taskService;
    private final AssignmentProperties properties;
    private final AssignmentStrategy assignmentStrategy;

    /**
     * Periodically check all active tasks against SLA configuration.
     * Tasks that exceed their SLA timeout are escalated to the configured supervisor group.
     */
    @Scheduled(fixedRateString = "${loanflow.assignment.sla-check-interval-ms:300000}")
    public void checkSlaBreaches() {
        if (!properties.isSlaEnabled()) {
            return;
        }

        properties.getSla().forEach((taskDefKey, slaConfig) -> {
            Instant deadline = Instant.now().minus(slaConfig.getTimeoutHours(), ChronoUnit.HOURS);

            List<Task> overdueTasks = taskService.createTaskQuery()
                    .taskDefinitionKey(taskDefKey)
                    .taskCreatedBefore(Date.from(deadline))
                    .list();

            if (!overdueTasks.isEmpty()) {
                log.info("SLA check: found {} overdue tasks for {} (SLA={}h)",
                        overdueTasks.size(), taskDefKey, slaConfig.getTimeoutHours());
            }

            overdueTasks.forEach(task -> escalateTask(task, slaConfig));
        });
    }

    /**
     * Escalate a single task by reassigning it to an officer from the escalation group.
     */
    void escalateTask(Task task, SlaConfig slaConfig) {
        String escalateToGroup = slaConfig.getEscalateTo();
        if (escalateToGroup == null || escalateToGroup.isBlank()) {
            log.warn("No escalation target configured for task {}, skipping", task.getTaskDefinitionKey());
            return;
        }

        Optional<String> newAssignee = assignmentStrategy.selectAssignee(escalateToGroup);
        newAssignee.ifPresent(userId -> {
            taskService.setAssignee(task.getId(), userId);
            taskService.addCandidateGroup(task.getId(), escalateToGroup);
            log.warn("SLA BREACH: Task {} ({}) escalated to group {} (assigned to {}). " +
                            "Created: {}, SLA: {}h",
                    task.getId(), task.getTaskDefinitionKey(),
                    escalateToGroup, userId,
                    task.getCreateTime(), slaConfig.getTimeoutHours());
        });

        if (newAssignee.isEmpty()) {
            log.error("SLA BREACH: Task {} ({}) — no officers available in escalation group {}",
                    task.getId(), task.getTaskDefinitionKey(), escalateToGroup);
        }
    }
}
