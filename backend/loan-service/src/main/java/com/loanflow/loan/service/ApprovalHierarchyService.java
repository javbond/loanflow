package com.loanflow.loan.service;

import com.loanflow.loan.domain.entity.ApprovalAuthority;
import com.loanflow.loan.domain.entity.DelegationOfAuthority;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.ApprovalAuthorityRepository;
import com.loanflow.loan.repository.DelegationOfAuthorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing the approval hierarchy matrix.
 *
 * Resolves which approval role (candidateGroup) is required for a given
 * loan type + amount combination. Also manages delegation of authority.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApprovalHierarchyService {

    private final ApprovalAuthorityRepository authorityRepository;
    private final DelegationOfAuthorityRepository delegationRepository;

    /** Default fallback role if no matching tier is found. */
    private static final String DEFAULT_ROLE = "UNDERWRITER";

    // ======================== TIER RESOLUTION ========================

    /**
     * Resolve the required approval role for a given loan type and amount.
     *
     * Resolution order:
     * 1. Product-specific tiers (exact loanType match) — highest priority
     * 2. Global tiers (loanType IS NULL) — fallback
     * 3. DEFAULT_ROLE — ultimate fallback
     *
     * @param loanType the loan product type
     * @param amount the requested loan amount
     * @return the required candidate group name (e.g., "SENIOR_UNDERWRITER")
     */
    public String resolveRequiredRole(LoanType loanType, BigDecimal amount) {
        if (loanType == null || amount == null) {
            log.warn("Null loanType or amount, returning default role: {}", DEFAULT_ROLE);
            return DEFAULT_ROLE;
        }

        List<ApprovalAuthority> matchingTiers = authorityRepository.findMatchingTiers(loanType, amount);

        if (!matchingTiers.isEmpty()) {
            ApprovalAuthority matched = matchingTiers.get(0);
            log.info("Resolved approval authority: loanType={}, amount={}, tier={}, role={}",
                    loanType, amount, matched.getTierName(), matched.getRequiredRole());
            return matched.getRequiredRole();
        }

        log.warn("No matching approval tier for loanType={}, amount={}. Using default: {}",
                loanType, amount, DEFAULT_ROLE);
        return DEFAULT_ROLE;
    }

    /**
     * Get all active tiers for a specific loan type (including global tiers).
     */
    public List<ApprovalAuthority> getActiveTiersForLoanType(LoanType loanType) {
        List<ApprovalAuthority> specific = authorityRepository
                .findByLoanTypeAndActiveTrueOrderByTierLevel(loanType);
        if (!specific.isEmpty()) {
            return specific;
        }
        // Fallback to global tiers
        return authorityRepository.findByActiveTrueOrderByLoanTypeAscTierLevelAsc()
                .stream()
                .filter(t -> t.getLoanType() == null)
                .toList();
    }

    /**
     * Get the complete approval matrix (all active tiers).
     */
    public List<ApprovalAuthority> getFullApprovalMatrix() {
        return authorityRepository.findByActiveTrueOrderByLoanTypeAscTierLevelAsc();
    }

    // ======================== AUTHORITY CRUD ========================

    /**
     * Create a new approval authority tier.
     */
    @Transactional
    public ApprovalAuthority createTier(ApprovalAuthority tier) {
        // Validate no overlapping tier exists
        Optional<ApprovalAuthority> existing = authorityRepository
                .findByLoanTypeAndTierLevel(tier.getLoanType(), tier.getTierLevel());
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Tier level %d already exists for loan type %s",
                            tier.getTierLevel(), tier.getLoanType()));
        }

        ApprovalAuthority saved = authorityRepository.save(tier);
        log.info("Created approval tier: {} (level={}, role={}, amount={}-{})",
                saved.getTierName(), saved.getTierLevel(), saved.getRequiredRole(),
                saved.getMinAmount(), saved.getMaxAmount());
        return saved;
    }

    /**
     * Update an existing approval authority tier.
     */
    @Transactional
    public ApprovalAuthority updateTier(UUID tierId, ApprovalAuthority updated) {
        ApprovalAuthority existing = authorityRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Approval tier not found: " + tierId));

        existing.setTierName(updated.getTierName());
        existing.setMinAmount(updated.getMinAmount());
        existing.setMaxAmount(updated.getMaxAmount());
        existing.setRequiredRole(updated.getRequiredRole());
        existing.setActive(updated.isActive());

        ApprovalAuthority saved = authorityRepository.save(existing);
        log.info("Updated approval tier: {} (id={})", saved.getTierName(), tierId);
        return saved;
    }

    /**
     * Deactivate a tier (soft delete).
     */
    @Transactional
    public void deactivateTier(UUID tierId) {
        ApprovalAuthority tier = authorityRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Approval tier not found: " + tierId));
        tier.setActive(false);
        authorityRepository.save(tier);
        log.info("Deactivated approval tier: {} (id={})", tier.getTierName(), tierId);
    }

    /**
     * Get a tier by ID.
     */
    public Optional<ApprovalAuthority> getTierById(UUID tierId) {
        return authorityRepository.findById(tierId);
    }

    // ======================== DELEGATION MANAGEMENT ========================

    /**
     * Create a new delegation of authority.
     */
    @Transactional
    public DelegationOfAuthority createDelegation(DelegationOfAuthority delegation) {
        if (delegation.getValidTo().isBefore(delegation.getValidFrom())) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        if (delegation.getDelegatorId().equals(delegation.getDelegateeId())) {
            throw new IllegalArgumentException("Delegator and delegatee must be different officers");
        }

        DelegationOfAuthority saved = delegationRepository.save(delegation);
        log.info("Created delegation: {} ({}) -> {} ({}) up to {} valid {}-{}",
                saved.getDelegatorId(), saved.getDelegatorRole(),
                saved.getDelegateeId(), saved.getDelegateeRole(),
                saved.getMaxAmount(), saved.getValidFrom(), saved.getValidTo());
        return saved;
    }

    /**
     * Revoke a delegation.
     */
    @Transactional
    public void revokeDelegation(UUID delegationId) {
        DelegationOfAuthority delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new IllegalArgumentException("Delegation not found: " + delegationId));
        delegation.setActive(false);
        delegationRepository.save(delegation);
        log.info("Revoked delegation: id={}", delegationId);
    }

    /**
     * Check if an officer has delegated authority for a given amount.
     */
    public boolean hasDelegatedAuthority(UUID officerId, BigDecimal amount) {
        return delegationRepository.hasDelegatedAuthority(officerId, LocalDate.now(), amount);
    }

    /**
     * Get all active delegations.
     */
    public List<DelegationOfAuthority> getActiveDelegations() {
        return delegationRepository.findByActiveTrue();
    }

    /**
     * Get active delegations by delegator.
     */
    public List<DelegationOfAuthority> getDelegationsByDelegator(UUID delegatorId) {
        return delegationRepository.findByDelegatorIdAndActiveTrue(delegatorId);
    }
}
