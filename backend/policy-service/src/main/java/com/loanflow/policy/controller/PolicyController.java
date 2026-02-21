package com.loanflow.policy.controller;

import com.loanflow.dto.request.PolicyRequest;
import com.loanflow.dto.response.ApiResponse;
import com.loanflow.dto.response.PolicyResponse;
import com.loanflow.policy.domain.enums.LoanType;
import com.loanflow.policy.domain.enums.PolicyCategory;
import com.loanflow.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for policy management
 */
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Policy Management", description = "APIs for managing loan policies")
public class PolicyController {

    private final PolicyService policyService;

    // ==================== CRUD Endpoints ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Create a new policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> create(
            @Valid @RequestBody PolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        PolicyResponse response = policyService.create(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Policy created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'LOAN_OFFICER', 'UNDERWRITER')")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<ApiResponse<PolicyResponse>> getById(@PathVariable String id) {
        PolicyResponse response = policyService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{policyCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'LOAN_OFFICER', 'UNDERWRITER')")
    @Operation(summary = "Get latest version of policy by code")
    public ResponseEntity<ApiResponse<PolicyResponse>> getByCode(@PathVariable String policyCode) {
        PolicyResponse response = policyService.getByPolicyCode(policyCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{policyCode}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get all versions of a policy")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getVersionHistory(@PathVariable String policyCode) {
        List<PolicyResponse> responses = policyService.getVersionHistory(policyCode);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Update a policy (DRAFT/INACTIVE only)")
    public ResponseEntity<ApiResponse<PolicyResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody PolicyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        PolicyResponse response = policyService.update(id, request, username);
        return ResponseEntity.ok(ApiResponse.success("Policy updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a policy (DRAFT only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        policyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Policy deleted successfully", null));
    }

    // ==================== Lifecycle Endpoints ====================

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Create a new version of a policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> createNewVersion(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        PolicyResponse response = policyService.createNewVersion(id, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("New policy version created", response));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Activate a policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> activate(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        PolicyResponse response = policyService.activate(id, username);
        return ResponseEntity.ok(ApiResponse.success("Policy activated", response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Deactivate a policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> deactivate(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        PolicyResponse response = policyService.deactivate(id, username);
        return ResponseEntity.ok(ApiResponse.success("Policy deactivated", response));
    }

    // ==================== Query Endpoints ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'LOAN_OFFICER', 'UNDERWRITER')")
    @Operation(summary = "List all policies with pagination")
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> listAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<PolicyResponse> response = policyService.listAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'LOAN_OFFICER', 'UNDERWRITER')")
    @Operation(summary = "List policies by category")
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> listByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {
        PolicyCategory cat = PolicyCategory.valueOf(category.toUpperCase());
        Page<PolicyResponse> response = policyService.listByCategory(cat, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'LOAN_OFFICER', 'UNDERWRITER')")
    @Operation(summary = "Search policies by text")
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyResponse> response = policyService.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/active/{loanType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'LOAN_OFFICER', 'UNDERWRITER')")
    @Operation(summary = "Get active policies for a loan type")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getActivePolicies(
            @PathVariable String loanType) {
        LoanType type = LoanType.valueOf(loanType.toUpperCase());
        List<PolicyResponse> response = policyService.getActivePoliciesForLoanType(type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get policy statistics")
    public ResponseEntity<ApiResponse<PolicyService.PolicyStatsResponse>> getStats() {
        PolicyService.PolicyStatsResponse stats = policyService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
