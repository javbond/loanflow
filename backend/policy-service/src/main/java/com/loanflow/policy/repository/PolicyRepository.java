package com.loanflow.policy.repository;

import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.LoanType;
import com.loanflow.policy.domain.enums.PolicyCategory;
import com.loanflow.policy.domain.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Policy aggregate
 */
@Repository
public interface PolicyRepository extends MongoRepository<Policy, String> {

    /**
     * Find all policies by status
     */
    Page<Policy> findByStatus(PolicyStatus status, Pageable pageable);

    /**
     * Find all policies by category and status
     */
    List<Policy> findByCategoryAndStatus(PolicyCategory category, PolicyStatus status);

    /**
     * Find all policies by loan type and status
     */
    List<Policy> findByLoanTypeAndStatus(LoanType loanType, PolicyStatus status);

    /**
     * Find active policies for a specific loan type (includes ALL type)
     */
    @Query("{ 'status': 'ACTIVE', '$or': [ { 'loanType': ?0 }, { 'loanType': 'ALL' } ] }")
    List<Policy> findActivePoliciesForLoanType(LoanType loanType);

    /**
     * Find active policies by category for a specific loan type
     */
    @Query("{ 'status': 'ACTIVE', 'category': ?0, '$or': [ { 'loanType': ?1 }, { 'loanType': 'ALL' } ] }")
    List<Policy> findActivePoliciesByCategoryAndLoanType(PolicyCategory category, LoanType loanType);

    /**
     * Find the latest version of a policy by its code
     */
    Optional<Policy> findFirstByPolicyCodeOrderByVersionNumberDesc(String policyCode);

    /**
     * Find all versions of a policy by its code
     */
    List<Policy> findByPolicyCodeOrderByVersionNumberDesc(String policyCode);

    /**
     * Find a specific version of a policy
     */
    Optional<Policy> findByPolicyCodeAndVersionNumber(String policyCode, Integer versionNumber);

    /**
     * Check if a policy name already exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Search policies by name or description containing text
     */
    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } } ] }")
    Page<Policy> searchByText(String searchText, Pageable pageable);

    /**
     * Find policies by category with pagination
     */
    Page<Policy> findByCategory(PolicyCategory category, Pageable pageable);

    /**
     * Find policies by tags
     */
    List<Policy> findByTagsContaining(String tag);

    /**
     * Count active policies by category
     */
    long countByCategoryAndStatus(PolicyCategory category, PolicyStatus status);
}
