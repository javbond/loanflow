package com.loanflow.loan.controller;

import com.loanflow.loan.domain.entity.ApprovalAuthority;
import com.loanflow.loan.domain.entity.DelegationOfAuthority;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.service.ApprovalHierarchyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing the approval authority matrix and
 * delegation of authority.
 *
 * Provides endpoints for:
 * - Viewing the approval matrix
 * - CRUD operations on approval tiers (ADMIN only)
 * - Resolving required role for a given loan type + amount
 * - Managing delegations of authority
 */
@RestController
@RequestMapping("/api/v1/approval-hierarchy")
@RequiredArgsConstructor
@Slf4j
public class ApprovalHierarchyController {

    private final ApprovalHierarchyService approvalService;

    // ======================== APPROVAL MATRIX ========================

    /**
     * Get the full approval authority matrix.
     */
    @GetMapping("/matrix")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'SENIOR_UNDERWRITER')")
    public ResponseEntity<List<ApprovalAuthorityResponse>> getApprovalMatrix() {
        List<ApprovalAuthority> matrix = approvalService.getFullApprovalMatrix();
        List<ApprovalAuthorityResponse> response = matrix.stream()
                .map(ApprovalAuthorityResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get approval tiers for a specific loan type.
     */
    @GetMapping("/matrix/{loanType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'SENIOR_UNDERWRITER', 'UNDERWRITER')")
    public ResponseEntity<List<ApprovalAuthorityResponse>> getTiersForLoanType(
            @PathVariable LoanType loanType) {
        List<ApprovalAuthority> tiers = approvalService.getActiveTiersForLoanType(loanType);
        List<ApprovalAuthorityResponse> response = tiers.stream()
                .map(ApprovalAuthorityResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Resolve the required approval role for a given loan type and amount.
     */
    @GetMapping("/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'SENIOR_UNDERWRITER', 'UNDERWRITER', 'LOAN_OFFICER')")
    public ResponseEntity<Map<String, String>> resolveRole(
            @RequestParam LoanType loanType,
            @RequestParam BigDecimal amount) {
        String role = approvalService.resolveRequiredRole(loanType, amount);
        return ResponseEntity.ok(Map.of(
                "loanType", loanType.name(),
                "amount", amount.toPlainString(),
                "requiredRole", role));
    }

    /**
     * Create a new approval tier.
     */
    @PostMapping("/matrix")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalAuthorityResponse> createTier(
            @RequestBody CreateTierRequest request) {
        ApprovalAuthority tier = ApprovalAuthority.builder()
                .loanType(request.loanType())
                .tierLevel(request.tierLevel())
                .tierName(request.tierName())
                .minAmount(request.minAmount())
                .maxAmount(request.maxAmount())
                .requiredRole(request.requiredRole())
                .active(true)
                .build();

        ApprovalAuthority created = approvalService.createTier(tier);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApprovalAuthorityResponse.from(created));
    }

    /**
     * Update an existing approval tier.
     */
    @PutMapping("/matrix/{tierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalAuthorityResponse> updateTier(
            @PathVariable UUID tierId,
            @RequestBody UpdateTierRequest request) {
        ApprovalAuthority updates = ApprovalAuthority.builder()
                .tierName(request.tierName())
                .minAmount(request.minAmount())
                .maxAmount(request.maxAmount())
                .requiredRole(request.requiredRole())
                .active(request.active())
                .build();

        ApprovalAuthority updated = approvalService.updateTier(tierId, updates);
        return ResponseEntity.ok(ApprovalAuthorityResponse.from(updated));
    }

    /**
     * Deactivate an approval tier (soft delete).
     */
    @DeleteMapping("/matrix/{tierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTier(@PathVariable UUID tierId) {
        approvalService.deactivateTier(tierId);
        return ResponseEntity.noContent().build();
    }

    // ======================== DELEGATION OF AUTHORITY ========================

    /**
     * Get all active delegations.
     */
    @GetMapping("/delegations")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<List<DelegationResponse>> getActiveDelegations() {
        List<DelegationOfAuthority> delegations = approvalService.getActiveDelegations();
        List<DelegationResponse> response = delegations.stream()
                .map(DelegationResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new delegation of authority.
     */
    @PostMapping("/delegations")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'SENIOR_UNDERWRITER')")
    public ResponseEntity<DelegationResponse> createDelegation(
            @RequestBody CreateDelegationRequest request) {
        DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                .delegatorId(request.delegatorId())
                .delegatorRole(request.delegatorRole())
                .delegateeId(request.delegateeId())
                .delegateeRole(request.delegateeRole())
                .maxAmount(request.maxAmount())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .reason(request.reason())
                .active(true)
                .build();

        DelegationOfAuthority created = approvalService.createDelegation(delegation);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DelegationResponse.from(created));
    }

    /**
     * Revoke a delegation.
     */
    @DeleteMapping("/delegations/{delegationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Void> revokeDelegation(@PathVariable UUID delegationId) {
        approvalService.revokeDelegation(delegationId);
        return ResponseEntity.noContent().build();
    }

    // ======================== DTOs ========================

    public record CreateTierRequest(
            LoanType loanType,
            int tierLevel,
            String tierName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String requiredRole
    ) {}

    public record UpdateTierRequest(
            String tierName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String requiredRole,
            boolean active
    ) {}

    public record CreateDelegationRequest(
            UUID delegatorId,
            String delegatorRole,
            UUID delegateeId,
            String delegateeRole,
            BigDecimal maxAmount,
            LocalDate validFrom,
            LocalDate validTo,
            String reason
    ) {}

    public record ApprovalAuthorityResponse(
            UUID id,
            String loanType,
            int tierLevel,
            String tierName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String requiredRole,
            boolean active
    ) {
        static ApprovalAuthorityResponse from(ApprovalAuthority entity) {
            return new ApprovalAuthorityResponse(
                    entity.getId(),
                    entity.getLoanType() != null ? entity.getLoanType().name() : "ALL",
                    entity.getTierLevel(),
                    entity.getTierName(),
                    entity.getMinAmount(),
                    entity.getMaxAmount(),
                    entity.getRequiredRole(),
                    entity.isActive());
        }
    }

    public record DelegationResponse(
            UUID id,
            UUID delegatorId,
            String delegatorRole,
            UUID delegateeId,
            String delegateeRole,
            BigDecimal maxAmount,
            LocalDate validFrom,
            LocalDate validTo,
            String reason,
            boolean active,
            boolean currentlyValid
    ) {
        static DelegationResponse from(DelegationOfAuthority entity) {
            return new DelegationResponse(
                    entity.getId(),
                    entity.getDelegatorId(),
                    entity.getDelegatorRole(),
                    entity.getDelegateeId(),
                    entity.getDelegateeRole(),
                    entity.getMaxAmount(),
                    entity.getValidFrom(),
                    entity.getValidTo(),
                    entity.getReason(),
                    entity.isActive(),
                    entity.isCurrentlyValid());
        }
    }
}
