package com.loanflow.loan.workflow.listener;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.workflow.assignment.ApprovalHierarchyResolver;
import com.loanflow.loan.workflow.assignment.AssignmentProperties;
import com.loanflow.loan.workflow.assignment.AssignmentStrategy;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Tests auto-assignment on task creation, disable toggle, entity sync,
 * and US-015 approval hierarchy integration.
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

    @Mock
    private ApprovalHierarchyResolver hierarchyResolver;

    @InjectMocks
    private AutoAssignmentTaskListener listener;

    @Mock
    private DelegateTask delegateTask;

    private static final String OFFICER_USER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    @BeforeEach
    void setUp() {
        lenient().when(delegateTask.getEventName()).thenReturn("create");
    }

    @Nested
    @DisplayName("Basic Assignment")
    class BasicAssignment {

        @Test
        @DisplayName("Should auto-assign task on create event using strategy")
        void shouldAutoAssignTaskOnCreate() {
            when(properties.isEnabled()).thenReturn(true);

            IdentityLink candidateLink = mock(IdentityLink.class);
            when(candidateLink.getGroupId()).thenReturn("LOAN_OFFICER");
            when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));
            when(delegateTask.getTaskDefinitionKey()).thenReturn("documentVerification");

            // Hierarchy resolver returns same group for non-underwriting tasks
            when(hierarchyResolver.resolveGroup("documentVerification", null, "LOAN_OFFICER"))
                    .thenReturn("LOAN_OFFICER");

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
            when(delegateTask.getTaskDefinitionKey()).thenReturn("documentVerification");

            UUID applicationId = UUID.randomUUID();
            when(delegateTask.getVariable("applicationId")).thenReturn(applicationId.toString());

            when(hierarchyResolver.resolveGroup("documentVerification", applicationId.toString(), "LOAN_OFFICER"))
                    .thenReturn("LOAN_OFFICER");

            when(assignmentStrategy.selectAssignee("LOAN_OFFICER"))
                    .thenReturn(Optional.of(OFFICER_USER_ID));

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

    @Nested
    @DisplayName("US-015: Approval Hierarchy Integration")
    class ApprovalHierarchyIntegration {

        @Test
        @DisplayName("Should use hierarchy-resolved group for underwriting tasks")
        void shouldUseHierarchyResolvedGroupForUnderwriting() {
            when(properties.isEnabled()).thenReturn(true);

            IdentityLink candidateLink = mock(IdentityLink.class);
            when(candidateLink.getGroupId()).thenReturn("UNDERWRITER");
            when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));
            when(delegateTask.getTaskDefinitionKey()).thenReturn("underwritingReview");

            UUID applicationId = UUID.randomUUID();
            when(delegateTask.getVariable("applicationId")).thenReturn(applicationId.toString());

            // Hierarchy resolver upgrades to SENIOR_UNDERWRITER based on amount
            when(hierarchyResolver.resolveGroup("underwritingReview", applicationId.toString(), "UNDERWRITER"))
                    .thenReturn("SENIOR_UNDERWRITER");

            String seniorUwId = "78861730-c75b-4cd6-b6d1-f5395136e492";
            when(assignmentStrategy.selectAssignee("SENIOR_UNDERWRITER"))
                    .thenReturn(Optional.of(seniorUwId));

            listener.notify(delegateTask);

            // Verify candidate group was updated
            verify(delegateTask).addCandidateGroup("SENIOR_UNDERWRITER");
            verify(delegateTask).deleteCandidateGroup("UNDERWRITER");
            verify(delegateTask).setAssignee(seniorUwId);
        }

        @Test
        @DisplayName("Should keep BPMN group when hierarchy returns same group")
        void shouldKeepBpmnGroupWhenHierarchyReturnsSame() {
            when(properties.isEnabled()).thenReturn(true);

            IdentityLink candidateLink = mock(IdentityLink.class);
            when(candidateLink.getGroupId()).thenReturn("UNDERWRITER");
            when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));
            when(delegateTask.getTaskDefinitionKey()).thenReturn("underwritingReview");

            UUID applicationId = UUID.randomUUID();
            when(delegateTask.getVariable("applicationId")).thenReturn(applicationId.toString());

            // Hierarchy resolver returns same group (amount within UNDERWRITER range)
            when(hierarchyResolver.resolveGroup("underwritingReview", applicationId.toString(), "UNDERWRITER"))
                    .thenReturn("UNDERWRITER");

            when(assignmentStrategy.selectAssignee("UNDERWRITER"))
                    .thenReturn(Optional.of(OFFICER_USER_ID));

            listener.notify(delegateTask);

            // Should NOT modify candidate groups
            verify(delegateTask, never()).addCandidateGroup(anyString());
            verify(delegateTask, never()).deleteCandidateGroup(anyString());
            verify(delegateTask).setAssignee(OFFICER_USER_ID);
        }

        @Test
        @DisplayName("Should route to BRANCH_MANAGER for very large loans")
        void shouldRouteToBranchManagerForLargeLoans() {
            when(properties.isEnabled()).thenReturn(true);

            IdentityLink candidateLink = mock(IdentityLink.class);
            when(candidateLink.getGroupId()).thenReturn("UNDERWRITER");
            when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));
            when(delegateTask.getTaskDefinitionKey()).thenReturn("underwritingReview");

            UUID applicationId = UUID.randomUUID();
            when(delegateTask.getVariable("applicationId")).thenReturn(applicationId.toString());

            // Very large loan → BRANCH_MANAGER
            when(hierarchyResolver.resolveGroup("underwritingReview", applicationId.toString(), "UNDERWRITER"))
                    .thenReturn("BRANCH_MANAGER");

            String managerId = "9a3c6949-7398-40bc-a512-1d049f43b9da";
            when(assignmentStrategy.selectAssignee("BRANCH_MANAGER"))
                    .thenReturn(Optional.of(managerId));

            listener.notify(delegateTask);

            verify(delegateTask).addCandidateGroup("BRANCH_MANAGER");
            verify(delegateTask).deleteCandidateGroup("UNDERWRITER");
            verify(delegateTask).setAssignee(managerId);
        }

        @Test
        @DisplayName("Should fallback to BPMN group when no applicationId available")
        void shouldFallbackWhenNoApplicationId() {
            when(properties.isEnabled()).thenReturn(true);

            IdentityLink candidateLink = mock(IdentityLink.class);
            when(candidateLink.getGroupId()).thenReturn("UNDERWRITER");
            when(delegateTask.getCandidates()).thenReturn(Set.of(candidateLink));
            when(delegateTask.getTaskDefinitionKey()).thenReturn("underwritingReview");

            // No applicationId available
            when(delegateTask.getVariable("applicationId")).thenReturn(null);

            when(hierarchyResolver.resolveGroup("underwritingReview", null, "UNDERWRITER"))
                    .thenReturn("UNDERWRITER");

            when(assignmentStrategy.selectAssignee("UNDERWRITER"))
                    .thenReturn(Optional.of(OFFICER_USER_ID));

            listener.notify(delegateTask);

            verify(delegateTask, never()).addCandidateGroup(anyString());
            verify(delegateTask).setAssignee(OFFICER_USER_ID);
        }
    }
}
