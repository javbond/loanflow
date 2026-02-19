package com.loanflow.loan.controller;

import com.loanflow.dto.common.ApiResponse;
import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Applications", description = "Loan application management APIs")
public class LoanApplicationController {

    private final LoanApplicationService service;

    @PostMapping
    @Operation(summary = "Create a new loan application")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> create(
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplicationResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Loan application created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan application by ID")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getById(
            @Parameter(description = "Application UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping("/number/{applicationNumber}")
    @Operation(summary = "Get loan application by application number")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getByApplicationNumber(
            @Parameter(description = "Application number (e.g., LN-2024-000001)")
            @PathVariable String applicationNumber) {
        return ResponseEntity.ok(ApiResponse.success(service.getByApplicationNumber(applicationNumber)));
    }

    @GetMapping
    @Operation(summary = "List all loan applications with pagination")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<LoanApplicationResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search loan applications by application number")
    public ResponseEntity<Page<LoanApplicationResponse>> search(
            @Parameter(description = "Search query (application number)") @RequestParam String query,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.searchByApplicationNumber(query, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get loan applications by customer ID")
    public ResponseEntity<Page<LoanApplicationResponse>> getByCustomerId(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getByCustomerId(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get loan applications by status")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<LoanApplicationResponse>> getByStatus(
            @PathVariable LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getByStatus(status, pageable));
    }

    @GetMapping("/assigned/{officerId}")
    @Operation(summary = "Get loan applications assigned to an officer")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<LoanApplicationResponse>> getByAssignedOfficer(
            @PathVariable UUID officerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getByAssignedOfficer(officerId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update loan application (only DRAFT or RETURNED status)")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.update(id, request),
                "Loan application updated successfully"));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit loan application for processing")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.submit(id),
                "Loan application submitted successfully"));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve loan application")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> approve(
            @PathVariable UUID id,
            @RequestParam BigDecimal approvedAmount,
            @RequestParam BigDecimal interestRate) {
        return ResponseEntity.ok(ApiResponse.success(
                service.approve(id, approvedAmount, interestRate),
                "Loan application approved successfully"));
    }

    @PostMapping("/{id}/conditional-approve")
    @Operation(summary = "Conditionally approve loan application")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> conditionallyApprove(
            @PathVariable UUID id,
            @RequestParam BigDecimal approvedAmount,
            @RequestParam BigDecimal interestRate,
            @RequestParam String conditions) {
        return ResponseEntity.ok(ApiResponse.success(
                service.conditionallyApprove(id, approvedAmount, interestRate, conditions),
                "Loan application conditionally approved"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject loan application")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> reject(
            @PathVariable UUID id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                service.reject(id, reason),
                "Loan application rejected"));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Return application for correction")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> returnForCorrection(
            @PathVariable UUID id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                service.returnForCorrection(id, reason),
                "Loan application returned for correction"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel loan application")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @RequestParam String reason) {
        service.cancel(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Loan application cancelled"));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign loan officer to application")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> assignOfficer(
            @PathVariable UUID id,
            @RequestParam UUID officerId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.assignOfficer(id, officerId),
                "Officer assigned successfully"));
    }

    @PostMapping("/{id}/transition")
    @Operation(summary = "Transition application status")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> transitionStatus(
            @PathVariable UUID id,
            @RequestParam LoanStatus newStatus) {
        return ResponseEntity.ok(ApiResponse.success(
                service.transitionStatus(id, newStatus),
                "Status transitioned successfully"));
    }

    @PatchMapping("/{id}/cibil")
    @Operation(summary = "Update CIBIL score for application")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> updateCibilScore(
            @PathVariable UUID id,
            @RequestParam Integer cibilScore,
            @RequestParam String riskCategory) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateCibilScore(id, cibilScore, riskCategory),
                "CIBIL score updated"));
    }
}
