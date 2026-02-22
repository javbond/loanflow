package com.loanflow.loan.workflow;

import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.dto.TaskResponse;
import com.loanflow.loan.workflow.impl.WorkflowServiceImpl;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowService Tests")
class WorkflowServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private AssignmentProperties assignmentProperties;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Start Process")
    class StartProcessTests {

        @Test
        @DisplayName("Should start process instance and return process ID")
        void shouldStartProcess() {
            ProcessInstance processInstance = mock(ProcessInstance.class);
            when(processInstance.getId()).thenReturn("proc-123");
            when(runtimeService.startProcessInstanceByKey(
                    eq("loanOrigination"), eq(applicationId.toString()), anyMap()))
                    .thenReturn(processInstance);

            Map<String, Object> variables = new HashMap<>();
            variables.put("applicationNumber", "LN-2024-000001");
            variables.put("loanType", "HOME_LOAN");

            String processId = workflowService.startProcess(applicationId, variables);

            assertThat(processId).isEqualTo("proc-123");
            verify(runtimeService).startProcessInstanceByKey(
                    eq("loanOrigination"), eq(applicationId.toString()), anyMap());
        }

        @Test
        @DisplayName("Should include applicationId in process variables")
        void shouldIncludeApplicationIdInVariables() {
            ProcessInstance processInstance = mock(ProcessInstance.class);
            when(processInstance.getId()).thenReturn("proc-456");
            when(runtimeService.startProcessInstanceByKey(anyString(), anyString(), anyMap()))
                    .thenReturn(processInstance);

            Map<String, Object> variables = new HashMap<>();
            workflowService.startProcess(applicationId, variables);

            assertThat(variables).containsEntry("applicationId", applicationId.toString());
        }
    }

    @Nested
    @DisplayName("Task Inbox")
    class TaskInboxTests {

        @Test
        @DisplayName("Should return tasks for candidate groups")
        void shouldGetTasksByRoles() {
            Task task = mockTask("task-1", "Document Verification", "documentVerification");

            TaskQuery taskQuery = mockTaskQuery(List.of(task), 1L);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskCandidateGroupIn(anyList())).thenReturn(taskQuery);
            when(taskQuery.ignoreAssigneeValue()).thenReturn(taskQuery);
            when(taskService.getVariables("task-1")).thenReturn(
                    Map.of("applicationId", applicationId.toString(),
                            "applicationNumber", "LN-2024-000001"));
            when(taskService.getIdentityLinksForTask("task-1")).thenReturn(List.of());

            Page<TaskResponse> result = workflowService.getTasksByRoles(
                    List.of("LOAN_OFFICER"), PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTaskName()).isEqualTo("Document Verification");
            assertThat(result.getContent().get(0).getApplicationNumber()).isEqualTo("LN-2024-000001");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return tasks claimed by assignee")
        void shouldGetTasksByAssignee() {
            Task task = mockTask("task-2", "Underwriting Review", "underwritingReview");
            when(task.getAssignee()).thenReturn("user-1");

            TaskQuery taskQuery = mockTaskQuery(List.of(task), 1L);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskAssignee("user-1")).thenReturn(taskQuery);
            when(taskService.getVariables("task-2")).thenReturn(
                    Map.of("applicationId", applicationId.toString()));
            when(taskService.getIdentityLinksForTask("task-2")).thenReturn(List.of());

            Page<TaskResponse> result = workflowService.getTasksByAssignee(
                    "user-1", PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAssignee()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("Should return empty page when no tasks")
        void shouldReturnEmptyPage() {
            TaskQuery taskQuery = mockTaskQuery(List.of(), 0L);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskCandidateGroupIn(anyList())).thenReturn(taskQuery);
            when(taskQuery.ignoreAssigneeValue()).thenReturn(taskQuery);

            Page<TaskResponse> result = workflowService.getTasksByRoles(
                    List.of("LOAN_OFFICER"), PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Complete Task")
    class CompleteTaskTests {

        @Test
        @DisplayName("Should complete task with variables")
        void shouldCompleteTask() {
            Task task = mock(Task.class);
            when(task.getAssignee()).thenReturn("user-1");

            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);

            Map<String, Object> variables = Map.of("decision", "APPROVED");

            workflowService.completeTask("task-1", variables, "user-1");

            verify(taskService).complete("task-1", variables);
            verify(taskService, never()).claim(anyString(), anyString());
        }

        @Test
        @DisplayName("Should claim and complete unclaimed task")
        void shouldClaimAndComplete() {
            Task task = mock(Task.class);
            when(task.getAssignee()).thenReturn(null);

            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);

            Map<String, Object> variables = Map.of("decision", "REJECTED");

            workflowService.completeTask("task-1", variables, "user-1");

            verify(taskService).claim("task-1", "user-1");
            verify(taskService).complete("task-1", variables);
        }

        @Test
        @DisplayName("Should throw when task not found")
        void shouldThrowWhenTaskNotFound() {
            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("missing")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(null);

            assertThatThrownBy(() ->
                    workflowService.completeTask("missing", Map.of(), "user-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Claim and Unclaim")
    class ClaimTests {

        @Test
        @DisplayName("Should claim unassigned task for user")
        void shouldClaimUnassignedTask() {
            Task task = mock(Task.class);
            when(task.getAssignee()).thenReturn(null);

            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);

            workflowService.claimTask("task-1", "user-1");

            verify(taskService).claim("task-1", "user-1");
        }

        @Test
        @DisplayName("Should no-op when task already assigned to same user")
        void shouldNoOpWhenAlreadyAssignedToSameUser() {
            Task task = mock(Task.class);
            when(task.getAssignee()).thenReturn("user-1");

            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);

            workflowService.claimTask("task-1", "user-1");

            verify(taskService, never()).claim(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw when task assigned to different user")
        void shouldThrowWhenAssignedToDifferentUser() {
            Task task = mock(Task.class);
            when(task.getAssignee()).thenReturn("other-user");

            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);

            assertThatThrownBy(() -> workflowService.claimTask("task-1", "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already claimed by another user");

            verify(taskService, never()).claim(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task does not exist")
        void shouldThrowWhenTaskNotFoundOnClaim() {
            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("missing")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(null);

            assertThatThrownBy(() -> workflowService.claimTask("missing", "user-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should unclaim task")
        void shouldUnclaimTask() {
            workflowService.unclaimTask("task-1");
            verify(taskService).unclaim("task-1");
        }
    }

    @Nested
    @DisplayName("Cancel Process")
    class CancelProcessTests {

        @Test
        @DisplayName("Should cancel running process")
        void shouldCancelProcess() {
            workflowService.cancelProcess("proc-123", "Customer cancelled");
            verify(runtimeService).deleteProcessInstance("proc-123", "Customer cancelled");
        }
    }

    @Nested
    @DisplayName("Get Task")
    class GetTaskTests {

        @Test
        @DisplayName("Should get task by ID")
        void shouldGetTaskById() {
            Task task = mockTask("task-1", "Document Verification", "documentVerification");

            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);
            when(taskService.getVariables("task-1")).thenReturn(
                    Map.of("applicationId", applicationId.toString()));
            when(taskService.getIdentityLinksForTask("task-1")).thenReturn(List.of());

            TaskResponse result = workflowService.getTask("task-1");

            assertThat(result.getTaskId()).isEqualTo("task-1");
            assertThat(result.getTaskName()).isEqualTo("Document Verification");
        }

        @Test
        @DisplayName("Should throw when task not found")
        void shouldThrowWhenNotFound() {
            TaskQuery taskQuery = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId("missing")).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(null);

            assertThatThrownBy(() -> workflowService.getTask("missing"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ===== Helper methods =====

    private Task mockTask(String id, String name, String definitionKey) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getName()).thenReturn(name);
        when(task.getTaskDefinitionKey()).thenReturn(definitionKey);
        when(task.getProcessInstanceId()).thenReturn("proc-123");
        when(task.getCreateTime()).thenReturn(new Date());
        return task;
    }

    private TaskQuery mockTaskQuery(List<Task> tasks, long count) {
        TaskQuery query = mock(TaskQuery.class);
        when(query.orderByTaskCreateTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.count()).thenReturn(count);
        when(query.listPage(anyInt(), anyInt())).thenReturn(tasks);
        return query;
    }
}
