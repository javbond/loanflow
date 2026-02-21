package com.loanflow.loan.workflow.listener;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.assignment.AssignmentStrategy;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for AutoAssignmentTaskListener.
 * Tests auto-assignment on task creation, disable toggle, and entity sync.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoAssignmentTaskListener Tests")
class AutoAssignmentTaskListenerTest {

    @Mock
    private AssignmentProperties properties;

    @Mock
    private AssignmentStrategy assignmentStrategy;

    @Mock
    private LoanApplicationRepository repository;

    @InjectMocks
    private AutoAssignmentTaskListener listener;

    @Mock
    private DelegateTask delegateTask;

    private static final String OFFICER_USER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    @BeforeEach
    void setUp() {
        lenient().when(delegateTask.getEventName()).thenReturn("create");
    }

    @Test
    @DisplayName("Should auto-assign task on create event using strategy")
    void shouldAutoAssignTaskOnCreate() {
        when(properties.isEnabled()).thenReturn(true);

        IdentityLink candidateLink = mock(IdentityLink.class);
        when(candidateLink.getGroupId()).thenReturn("LOAN_OFFICER");
        when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));

        when(assignmentStrategy.selectAssignee("LOAN_OFFICER"))
                .thenReturn(Optional.of(OFFICER_USER_ID));

        listener.notify(delegateTask);

        verify(delegateTask).setAssignee(OFFICER_USER_ID);
    }

    @Test
    @DisplayName("Should skip assignment when disabled")
    void shouldSkipWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        listener.notify(delegateTask);

        verify(delegateTask, never()).setAssignee(anyString());
        verify(assignmentStrategy, never()).selectAssignee(anyString());
    }

    @Test
    @DisplayName("Should skip when no candidate groups on task")
    void shouldSkipWhenNoCandidateGroups() {
        when(properties.isEnabled()).thenReturn(true);
        when(delegateTask.getCandidates()).thenReturn(Set.of());

        listener.notify(delegateTask);

        verify(assignmentStrategy, never()).selectAssignee(anyString());
        verify(delegateTask, never()).setAssignee(anyString());
    }

    @Test
    @DisplayName("Should sync LoanApplication.assignedOfficer on assignment")
    void shouldSyncLoanApplicationAssignedOfficer() {
        when(properties.isEnabled()).thenReturn(true);

        IdentityLink candidateLink = mock(IdentityLink.class);
        when(candidateLink.getGroupId()).thenReturn("LOAN_OFFICER");
        when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));

        when(assignmentStrategy.selectAssignee("LOAN_OFFICER"))
                .thenReturn(Optional.of(OFFICER_USER_ID));

        UUID applicationId = UUID.randomUUID();
        when(delegateTask.getVariable("applicationId")).thenReturn(applicationId.toString());

        LoanApplication application = LoanApplication.builder()
                .id(applicationId)
                .build();
        when(repository.findById(applicationId)).thenReturn(Optional.of(application));

        listener.notify(delegateTask);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAssignedOfficer()).isEqualTo(UUID.fromString(OFFICER_USER_ID));
    }
}
