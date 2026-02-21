package com.loanflow.loan.workflow.assignment;

import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for assignment strategies.
 * Tests round-robin cycling and workload-based selection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Assignment Strategy Tests")
class AssignmentStrategyTest {

    @Nested
    @DisplayName("RoundRobinAssignmentStrategy")
    class RoundRobinTests {

        private AssignmentProperties properties;
        private RoundRobinAssignmentStrategy strategy;

        @BeforeEach
        void setUp() {
            properties = new AssignmentProperties();
            properties.setOfficers(Map.of(
                    "LOAN_OFFICER", List.of("officer-1", "officer-2", "officer-3"),
                    "UNDERWRITER", List.of("underwriter-1")
            ));
            strategy = new RoundRobinAssignmentStrategy(properties);
        }

        @Test
        @DisplayName("Should cycle through officers in round-robin order")
        void shouldCycleThroughOfficers() {
            Optional<String> first = strategy.selectAssignee("LOAN_OFFICER");
            Optional<String> second = strategy.selectAssignee("LOAN_OFFICER");
            Optional<String> third = strategy.selectAssignee("LOAN_OFFICER");

            assertThat(first).isPresent().hasValue("officer-1");
            assertThat(second).isPresent().hasValue("officer-2");
            assertThat(third).isPresent().hasValue("officer-3");
        }

        @Test
        @DisplayName("Should wrap around to first officer after reaching end")
        void shouldWrapAround() {
            strategy.selectAssignee("LOAN_OFFICER"); // officer-1
            strategy.selectAssignee("LOAN_OFFICER"); // officer-2
            strategy.selectAssignee("LOAN_OFFICER"); // officer-3

            Optional<String> fourth = strategy.selectAssignee("LOAN_OFFICER");
            assertThat(fourth).isPresent().hasValue("officer-1");
        }

        @Test
        @DisplayName("Should return empty when no officers configured for group")
        void shouldReturnEmptyForUnconfiguredGroup() {
            Optional<String> result = strategy.selectAssignee("NONEXISTENT_GROUP");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle single officer in group")
        void shouldHandleSingleOfficer() {
            Optional<String> first = strategy.selectAssignee("UNDERWRITER");
            Optional<String> second = strategy.selectAssignee("UNDERWRITER");

            assertThat(first).isPresent().hasValue("underwriter-1");
            assertThat(second).isPresent().hasValue("underwriter-1");
        }
    }

    @Nested
    @DisplayName("WorkloadBasedAssignmentStrategy")
    class WorkloadBasedTests {

        @Mock
        private TaskService taskService;

        private AssignmentProperties properties;
        private WorkloadBasedAssignmentStrategy strategy;

        @BeforeEach
        void setUp() {
            properties = new AssignmentProperties();
            properties.setOfficers(Map.of(
                    "LOAN_OFFICER", List.of("officer-1", "officer-2", "officer-3")
            ));
            strategy = new WorkloadBasedAssignmentStrategy(properties, taskService);
        }

        @Test
        @DisplayName("Should select officer with fewest active tasks")
        void shouldSelectOfficerWithFewestTasks() {
            // officer-1: 3 tasks, officer-2: 1 task, officer-3: 5 tasks
            TaskQuery query = mock(TaskQuery.class);
            when(taskService.createTaskQuery()).thenReturn(query);
            when(query.taskAssignee(anyString())).thenReturn(query);
            when(query.count())
                    .thenReturn(3L)   // officer-1
                    .thenReturn(1L)   // officer-2
                    .thenReturn(5L);  // officer-3

            Optional<String> result = strategy.selectAssignee("LOAN_OFFICER");

            assertThat(result).isPresent().hasValue("officer-2");
        }

        @Test
        @DisplayName("Should return empty when no officers configured")
        void shouldReturnEmptyWhenNoOfficers() {
            Optional<String> result = strategy.selectAssignee("NONEXISTENT_GROUP");
            assertThat(result).isEmpty();
        }
    }
}
