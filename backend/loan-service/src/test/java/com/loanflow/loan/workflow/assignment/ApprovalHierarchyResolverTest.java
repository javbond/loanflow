package com.loanflow.loan.workflow.assignment;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.ApprovalHierarchyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for ApprovalHierarchyResolver.
 * Tests dynamic candidate group resolution for underwriting tasks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalHierarchyResolver Tests")
class ApprovalHierarchyResolverTest {

    @Mock
    private ApprovalHierarchyService approvalService;

    @Mock
    private LoanApplicationRepository applicationRepository;

    @InjectMocks
    private ApprovalHierarchyResolver resolver;

    @Nested
    @DisplayName("Hierarchy Applicability")
    class HierarchyApplicability {

        @Test
        @DisplayName("Should apply hierarchy to underwritingReview task")
        void shouldApplyToUnderwritingReview() {
            assertThat(resolver.isHierarchyApplicable("underwritingReview")).isTrue();
        }

        @Test
        @DisplayName("Should apply hierarchy to referredReview task")
        void shouldApplyToReferredReview() {
            assertThat(resolver.isHierarchyApplicable("referredReview")).isTrue();
        }

        @Test
        @DisplayName("Should NOT apply hierarchy to documentVerification task")
        void shouldNotApplyToDocumentVerification() {
            assertThat(resolver.isHierarchyApplicable("documentVerification")).isFalse();
        }

        @Test
        @DisplayName("Should NOT apply hierarchy to unknown task")
        void shouldNotApplyToUnknownTask() {
            assertThat(resolver.isHierarchyApplicable("someOtherTask")).isFalse();
        }
    }

    @Nested
    @DisplayName("Group Resolution")
    class GroupResolution {

        @Test
        @DisplayName("Should resolve to LOAN_OFFICER for small loan (₹3L)")
        void shouldResolveLoanOfficerForSmallLoan() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = LoanApplication.builder()
                    .id(appId)
                    .applicationNumber("LN-2025-000001")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("300000"))
                    .build();

            when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
            when(approvalService.resolveRequiredRole(LoanType.PERSONAL_LOAN, new BigDecimal("300000")))
                    .thenReturn("LOAN_OFFICER");

            String group = resolver.resolveGroup("underwritingReview", appId.toString(), "UNDERWRITER");

            assertThat(group).isEqualTo("LOAN_OFFICER");
        }

        @Test
        @DisplayName("Should resolve to SENIOR_UNDERWRITER for large loan (₹50L)")
        void shouldResolveSeniorUnderwriterForLargeLoan() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = LoanApplication.builder()
                    .id(appId)
                    .applicationNumber("LN-2025-000002")
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("5000000"))
                    .build();

            when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
            when(approvalService.resolveRequiredRole(LoanType.PERSONAL_LOAN, new BigDecimal("5000000")))
                    .thenReturn("SENIOR_UNDERWRITER");

            String group = resolver.resolveGroup("underwritingReview", appId.toString(), "UNDERWRITER");

            assertThat(group).isEqualTo("SENIOR_UNDERWRITER");
        }

        @Test
        @DisplayName("Should resolve to BRANCH_MANAGER for very large loan (₹2Cr)")
        void shouldResolveBranchManagerForVeryLargeLoan() {
            UUID appId = UUID.randomUUID();
            LoanApplication app = LoanApplication.builder()
                    .id(appId)
                    .applicationNumber("LN-2025-000003")
                    .loanType(LoanType.HOME_LOAN)
                    .requestedAmount(new BigDecimal("25000000"))
                    .build();

            when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
            when(approvalService.resolveRequiredRole(LoanType.HOME_LOAN, new BigDecimal("25000000")))
                    .thenReturn("BRANCH_MANAGER");

            String group = resolver.resolveGroup("underwritingReview", appId.toString(), "UNDERWRITER");

            assertThat(group).isEqualTo("BRANCH_MANAGER");
        }

        @Test
        @DisplayName("Should use default group for non-hierarchy tasks")
        void shouldUseDefaultForNonHierarchyTasks() {
            String group = resolver.resolveGroup("documentVerification", UUID.randomUUID().toString(), "LOAN_OFFICER");

            assertThat(group).isEqualTo("LOAN_OFFICER");
            verify(applicationRepository, never()).findById(any());
            verify(approvalService, never()).resolveRequiredRole(any(), any());
        }

        @Test
        @DisplayName("Should use default group when applicationId is null")
        void shouldUseDefaultWhenApplicationIdNull() {
            String group = resolver.resolveGroup("underwritingReview", null, "UNDERWRITER");

            assertThat(group).isEqualTo("UNDERWRITER");
            verify(applicationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should use default group when applicationId is blank")
        void shouldUseDefaultWhenApplicationIdBlank() {
            String group = resolver.resolveGroup("underwritingReview", "", "UNDERWRITER");

            assertThat(group).isEqualTo("UNDERWRITER");
        }

        @Test
        @DisplayName("Should use default group when application not found")
        void shouldUseDefaultWhenApplicationNotFound() {
            UUID appId = UUID.randomUUID();
            when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

            String group = resolver.resolveGroup("underwritingReview", appId.toString(), "UNDERWRITER");

            assertThat(group).isEqualTo("UNDERWRITER");
        }

        @Test
        @DisplayName("Should use default group when applicationId is invalid UUID")
        void shouldUseDefaultWhenInvalidUUID() {
            String group = resolver.resolveGroup("underwritingReview", "not-a-uuid", "UNDERWRITER");

            assertThat(group).isEqualTo("UNDERWRITER");
        }
    }
}
