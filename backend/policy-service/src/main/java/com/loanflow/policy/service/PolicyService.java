package com.loanflow.policy.service;

import com.loanflow.dto.request.PolicyRequest;
import com.loanflow.dto.response.PolicyResponse;
import com.loanflow.policy.domain.enums.LoanType;
import com.loanflow.policy.domain.enums.PolicyCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for policy operations
 */
public interface PolicyService {

    /**
     * Create a new policy
     */
    PolicyResponse create(PolicyRequest request, String createdBy);

    /**
     * Get policy by ID
     */
    PolicyResponse getById(String id);

    /**
     * Get the latest version of a policy by its code
     */
    PolicyResponse getByPolicyCode(String policyCode);

    /**
     * Get all versions of a policy
     */
    List<PolicyResponse> getVersionHistory(String policyCode);

    /**
     * Update a policy (only DRAFT or INACTIVE policies)
     */
    PolicyResponse update(String id, PolicyRequest request, String modifiedBy);

    /**
     * Create a new version of an existing policy
     */
    PolicyResponse createNewVersion(String id, String createdBy);

    /**
     * Activate a policy
     */
    PolicyResponse activate(String id, String modifiedBy);

    /**
     * Deactivate a policy
     */
    PolicyResponse deactivate(String id, String modifiedBy);

    /**
     * Delete a policy (only DRAFT policies)
     */
    void delete(String id);

    /**
     * List all policies with pagination
     */
    Page<PolicyResponse> listAll(Pageable pageable);

    /**
     * List policies by category
     */
    Page<PolicyResponse> listByCategory(PolicyCategory category, Pageable pageable);

    /**
     * Search policies by text
     */
    Page<PolicyResponse> search(String searchText, Pageable pageable);

    /**
     * Get active policies for a loan type (used by evaluation engine)
     */
    List<PolicyResponse> getActivePoliciesForLoanType(LoanType loanType);

    /**
     * Get active policies by category and loan type
     */
    List<PolicyResponse> getActivePoliciesByCategoryAndLoanType(PolicyCategory category, LoanType loanType);

    /**
     * Get policy statistics (count by category and status)
     */
    PolicyStatsResponse getStats();

    /**
     * Inner class for stats response
     */
    record PolicyStatsResponse(
            long totalPolicies,
            long activePolicies,
            long draftPolicies,
            long inactivePolicies,
            java.util.Map<String, Long> byCategory
    ) {}
}
