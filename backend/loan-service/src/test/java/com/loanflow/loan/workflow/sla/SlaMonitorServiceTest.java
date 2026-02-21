package com.loanflow.loan.workflow.sla;

import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.assignment.AssignmentProperties.SlaConfig;
import com.loanflow.loan.workflow.assignment.AssignmentStrategy;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for SlaMonitorService.
 * Tests SLA breach detection and escalation behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SLA Monitor Tests")
class SlaMonitorServiceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private AssignmentStrategy assignmentStrategy;

    private AssignmentProperties properties;
    private SlaMonitorService slaMonitorService;

    @BeforeEach
    void setUp() {
        properties = new AssignmentProperties();
        slaMonitorService = new SlaMonitorService(taskService, properties, assignmentStrategy);
    }

    @Test
    @DisplayName("Should escalate tasks that breach SLA timeout")
    void shouldEscalateBreachedTasks() {
        // Configure SLA: documentVerification = 24h, escalate to SENIOR_UNDERWRITER
        properties.setSlaEnabled(true);
        SlaConfig slaConfig = new SlaConfig();
        slaConfig.setTimeoutHours(24);
        slaConfig.setEscalateTo("SENIOR_UNDERWRITER");
        properties.setSla(Map.of("documentVerification", slaConfig));

        // Mock: query returns one overdue task
        Task overdueTask = mock(Task.class);
        when(overdueTask.getId()).thenReturn("task-overdue-1");
        when(overdueTask.getTaskDefinitionKey()).thenReturn("documentVerification");

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskDefinitionKey("documentVerification")).thenReturn(taskQuery);
        when(taskQuery.taskCreatedBefore(any(Date.class))).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(overdueTask));

        // Mock: strategy selects an officer from escalation group
        when(assignmentStrategy.selectAssignee("SENIOR_UNDERWRITER"))
                .thenReturn(Optional.of("senior-uuid-1"));

        slaMonitorService.checkSlaBreaches();

        // Verify: task was reassigned to escalation target
        verify(taskService).setAssignee("task-overdue-1", "senior-uuid-1");
        verify(taskService).addCandidateGroup("task-overdue-1", "SENIOR_UNDERWRITER");
    }

    @Test
    @DisplayName("Should not escalate tasks within SLA window")
    void shouldNotEscalateWithinSla() {
        // Configure SLA: documentVerification = 48h
        properties.setSlaEnabled(true);
        SlaConfig slaConfig = new SlaConfig();
        slaConfig.setTimeoutHours(48);
        slaConfig.setEscalateTo("SENIOR_UNDERWRITER");
        properties.setSla(Map.of("documentVerification", slaConfig));

        // Mock: query returns no overdue tasks (all within SLA)
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskDefinitionKey("documentVerification")).thenReturn(taskQuery);
        when(taskQuery.taskCreatedBefore(any(Date.class))).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of()); // No overdue tasks

        slaMonitorService.checkSlaBreaches();

        // Verify: no escalation actions
        verify(taskService, never()).setAssignee(anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip when SLA monitoring is disabled")
    void shouldSkipWhenDisabled() {
        properties.setSlaEnabled(false);

        slaMonitorService.checkSlaBreaches();

        // Verify: no TaskService queries made
        verify(taskService, never()).createTaskQuery();
    }
}
