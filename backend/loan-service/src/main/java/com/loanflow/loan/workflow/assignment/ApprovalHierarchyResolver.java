package com.loanflow.loan.workflow.assignment;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.ApprovalHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the correct candidate group for underwriting tasks based on the
 * loan application's amount and type, using the approval authority matrix.
 *
 * Used by AutoAssignmentTaskListener to override the BPMN-hardcoded
 * candidateGroup with the dynamically resolved one.
 *
 * Resolution logic:
 * 1. Look up the loan application from process variables
 * 2. Query ApprovalHierarchyService for the required approval role
 * 3. Return the resolved role as the candidate group
 *
 * Falls back to the original BPMN candidateGroup if the application is not found
 * or if no matching tier exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalHierarchyResolver {

    private final ApprovalHierarchyService approvalService;
    private final LoanApplicationRepository applicationRepository;

    /** Task definition keys that support dynamic approval hierarchy routing. */
    private static final String UNDERWRITING_REVIEW_TASK = "underwritingReview";
    private static final String REFERRED_REVIEW_TASK = "referredReview";

    /**
     * Resolve the appropriate candidate group for a given task, based on
     * the loan application's amount and the approval matrix.
     *
     * @param taskDefinitionKey the BPMN task definition key (e.g., "underwritingReview")
     * @param applicationId the loan application UUID (from process variable)
     * @param defaultGroup the BPMN-defined candidate group to use as fallback
     * @return the resolved candidate group name
     */
    public String resolveGroup(String taskDefinitionKey, String applicationId, String defaultGroup) {
        // Only apply hierarchy routing to underwriting tasks
        if (!isHierarchyApplicable(taskDefinitionKey)) {
            log.debug("Task {} does not support hierarchy routing, using default: {}",
                    taskDefinitionKey, defaultGroup);
            return defaultGroup;
        }

        if (applicationId == null || applicationId.isBlank()) {
            log.warn("No applicationId available for task {}, using default: {}",
                    taskDefinitionKey, defaultGroup);
            return defaultGroup;
        }

        try {
            UUID appId = UUID.fromString(applicationId);
            Optional<LoanApplication> applicationOpt = applicationRepository.findById(appId);

            if (applicationOpt.isEmpty()) {
                log.warn("Loan application {} not found, using default group: {}",
                        applicationId, defaultGroup);
                return defaultGroup;
            }

            LoanApplication application = applicationOpt.get();
            LoanType loanType = application.getLoanType();
            BigDecimal amount = application.getRequestedAmount();

            String resolvedRole = approvalService.resolveRequiredRole(loanType, amount);

            log.info("Approval hierarchy resolved: task={}, app={}, type={}, amount={} → role={}",
                    taskDefinitionKey, application.getApplicationNumber(),
                    loanType, amount, resolvedRole);

            return resolvedRole;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid applicationId '{}', using default group: {}", applicationId, defaultGroup);
            return defaultGroup;
        }
    }

    /**
     * Check if a task definition key supports dynamic hierarchy routing.
     */
    public boolean isHierarchyApplicable(String taskDefinitionKey) {
        return UNDERWRITING_REVIEW_TASK.equals(taskDefinitionKey)
                || REFERRED_REVIEW_TASK.equals(taskDefinitionKey);
    }
}
